package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.PutMode
import com.github.yoshiyoshifujii.aws.apigateway.{AWSApiGatewayAuthorize, AWSApiGatewayMethods, RequestTemplates, RestApiId}
import serverless.{AuthorizeEvent, HttpEvent, ServerlessOption, Function => ServerlessFunction}

import scala.util.Try

trait DeployBase extends DeployFunctionBase {
  val name: String
  val description: Option[String]
  val version: Option[String]

  private def getOrCreateRestApi =
    for {
      restApiId <- {
        so.provider.restApiId map { id =>
          Try(id)
        } getOrElse {
          api.create(
            name = name,
            description = description
          ).map(_.getId)
        }
      }
      _ = { println(s"API Gateway created: $restApiId") }
    } yield restApiId

  private def putRestApi(restApiId: RestApiId) =
    for {
      putRestApiResult <- api.put(
        restApiId = restApiId,
        body = so.provider.swagger,
        mode = PutMode.Merge,
        failOnWarnings = None
      )
      _ = { println(s"API Gateway put: ${putRestApiResult.getId}") }
    } yield putRestApiResult

  private def createDeployment(restApiId: RestApiId,
                                     stage: String) =
    for {
      createDeploymentResult <- api.createDeployment(
        restApiId = restApiId,
        stageName = stage,
        stageDescription = None,
        description = version,
        variables = so.provider.getStageVariables(stage)
      )
      _ = { println(s"Create Deployment: ${createDeploymentResult.toString}") }
    } yield createDeploymentResult

  protected def publishVersion(function: ServerlessFunction) =
    for {
      publishVersionResult <- lambda.publishVersion(
        functionName = function.name,
        description = version
      )
      _ = { println(s"Lambda published: ${publishVersionResult.getFunctionArn}") }
    } yield publishVersionResult.getVersion

  private def deployAlias(function: ServerlessFunction,
                                aliasName: String,
                                functionVersion: Option[String],
                                description: Option[String]) =
    for {
      aOp <- lambda.getAlias(function.name, aliasName)
      aliasArn <- aOp map { _ =>
        lambda.updateAlias(
          functionName = function.name,
          name = aliasName,
          functionVersion = functionVersion,
          description = description
        ).map(_.getAliasArn)
      } getOrElse {
        for {
          a <- lambda.createAlias(
            functionName = function.name,
            name = aliasName,
            functionVersion = functionVersion,
            description = description
          )
          _ <- lambda.addPermission(
            functionArn = a.getAliasArn
          )
        } yield a.getAliasArn
      }
      _ = { println(s"Lambda Alias: $aliasArn") }
    } yield aliasArn

  private def deployResource(restApiId: RestApiId,
                                   function: ServerlessFunction,
                                   lambdaAlias: Option[String],
                                   httpEvent: HttpEvent) = {
    val method = AWSApiGatewayMethods(
      regionName = so.provider.region,
      restApiId = restApiId,
      path = httpEvent.path,
      httpMethod = httpEvent.method)

    for {
      resourceOpt <- method.deploy(
        awsAccountId = so.provider.awsAccount,
        lambdaName = function.name,
        lambdaAlias = lambdaAlias,
        requestTemplates = RequestTemplates(httpEvent.request.templateToSeq: _*),
        responseTemplates = httpEvent.response.templates,
        withAuth = withAuth(method)(
          AWSApiGatewayAuthorize(so.provider.region, restApiId))(
          httpEvent.authorizerName),
        cors = httpEvent.cors
      )
      _ = { resourceOpt.foreach(r => println(s"Resource: ${r.toString}")) }
    } yield ()
  }

  private def deployAuthorizer(restApiId: RestApiId,
                                     lambdaAlias: Option[String],
                                     authorizeEvent: AuthorizeEvent) = {
    lazy val authorize = AWSApiGatewayAuthorize(
      so.provider.region, restApiId
    )
    for {
      authId <- authorize.deployAuthorizer(
        name = authorizeEvent.name,
        awsAccountId = so.provider.awsAccount,
        lambdaName = authorizeEvent.name,
        lambdaAlias = lambdaAlias,
        identitySourceHeaderName = authorizeEvent.identitySourceHeaderName,
        identityValidationExpression = authorizeEvent.identityValidationExpression,
        authorizerResultTtlInSeconds = Option(authorizeEvent.resultTtlInSeconds)
      )
      _ = { println(s"Authorizer: $authId") }
    } yield ()
  }

  private def deployEvents(restApiId: RestApiId,
                                 function: ServerlessFunction,
                                 publishedVersion: String) =
    for {
      _ <- sequence {
        function.events.httpEventsMap { httpEvent =>
          deployResource(
            restApiId = restApiId,
            function = function,
            lambdaAlias = Option(s"${httpEvent.uriLambdaAlias}$publishedVersion"),
            httpEvent = httpEvent
          )
        }
      }
      _ <- sequence {
        function.events.authorizeEventMap { authorizeEvent =>
          deployAuthorizer(
            restApiId = restApiId,
            lambdaAlias = Option(s"${authorizeEvent.uriLambdaAlias}$publishedVersion"),
            authorizeEvent = authorizeEvent
          )
        }
      }
    } yield ()

  protected def generateFunctionVersion(publishedVersion: String) =
    Option(publishedVersion)

  private def invokeFunctions(restApiId: RestApiId,
                              stage: String) =
    sequence {
      so.functions.map { function =>
        for {
          _ <- invoke(function)
          publishedVersion <- publishVersion(function)
          _ <- deployAlias(
            function = function,
            aliasName = s"$stage$publishedVersion",
            functionVersion = generateFunctionVersion(publishedVersion),
            description = version
          )
          _ <- deployEvents(
            restApiId = restApiId,
            function = function,
            publishedVersion = publishedVersion
          )
        } yield ()
      }
    }

  def invoke(stage: String): Try[Unit] =
    for {
      restApiId <- getOrCreateRestApi
      _ <- putRestApi(restApiId)
      _ <- invokeFunctions(restApiId, stage)
      _ <- createDeployment(restApiId, stage)
    } yield ()

}

case class Deploy(so: ServerlessOption,
                  name: String,
                  description: Option[String],
                  version: Option[String]) extends DeployBase

