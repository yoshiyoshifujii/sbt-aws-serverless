package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.PutMode
import com.github.yoshiyoshifujii.aws.apigateway.{AWSApiGatewayAuthorize, AWSApiGatewayMethods, AWSApiGatewayRestApi, RequestTemplates}
import serverless.ServerlessOption

import scala.util.Try

trait DeployBase extends DeployFunctionBase {
  val name: String
  val description: Option[String]
  val version: Option[String]

  def invoke: Try[Unit] = {

    val api = AWSApiGatewayRestApi(so.provider.region)

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

      putRestApiResult <- api.put(
        restApiId = restApiId,
        body = so.provider.swagger,
        mode = PutMode.Merge,
        failOnWarnings = None
      )
      _ = { println(s"API Gateway put: ${putRestApiResult.getId}") }

      _ <- sequence {
        lazy val authorize = AWSApiGatewayAuthorize(
          so.provider.region, restApiId
        )
        so.functions.map { function =>
          for {
            functionArn <- deployFunction(function)

            publishVersionResult <- lambda.publishVersion(
              functionName = function.name,
              description = version
            )
            _ = { println(s"Lambda published: ${publishVersionResult.getFunctionArn}") }

            aliasArn <- deployAlias(
              lambda = lambda,
              functionName = function.name,
              aliasName = so.provider.stage,
              functionVersion = Option(publishVersionResult.getVersion),
              description = version
            )
            _ = { println(s"Lambda Alias: $aliasArn") }

            _ <- sequence {
              function.events.httpEventsMap { httpEvent =>

                val method = AWSApiGatewayMethods(
                  regionName = so.provider.region,
                  restApiId = restApiId,
                  path = httpEvent.path,
                  httpMethod = httpEvent.method)

                for {
                  _ <- swap {
                    httpEvent.authorizer.map { au =>
                      for {
                        authorizerId <- authorize.deployAuthorizer(
                          name = au.name,
                          awsAccountId = so.provider.awsAccount,
                          lambdaName = au.name,
                          lambdaAlias = None,
                          identitySourceHeaderName = au.identitySourceHeaderName,
                          identityValidationExpression = au.identityValidationExpression,
                          authorizerResultTtlInSeconds = Option(au.resultTtlInSeconds)
                        )
                        _ = { println(s"Authorizer: $authorizerId") }
                      } yield ()
                    }
                  }
                  resourceOpt <- method.deploy(
                    awsAccountId = so.provider.awsAccount,
                    lambdaName = function.name,
                    lambdaAlias = Option(s"${httpEvent.uriLambdaAlias}${publishVersionResult.getVersion}"),
                    requestTemplates = RequestTemplates(httpEvent.request.templateToSeq: _*),
                    responseTemplates = httpEvent.response.statusCodes,
                    withAuth(method)(
                      AWSApiGatewayAuthorize(so.provider.region, restApiId))(
                      httpEvent.authorizer.map(_.name))
                  )
                  _ = { resourceOpt.foreach(r => println(s"Resource: ${r.toString}")) }
                } yield ()
              }
            }

          } yield ()
        }
      }

      createDeploymentResult <- api.createDeployment(
        restApiId = restApiId,
        stageName = so.provider.stage,
        stageDescription = None,
        description = version,
        variables = so.provider.getStageVariables
      )
      _ = { println(s"Create Deployment: ${createDeploymentResult.toString}") }

    } yield ()
  }

}

case class Deploy(so: ServerlessOption,
                  name: String,
                  description: Option[String],
                  version: Option[String]) extends DeployBase

