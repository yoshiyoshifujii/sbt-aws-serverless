package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize
import com.amazonaws.services.apigateway.model.PutMode
import com.github.yoshiyoshifujii.aws.apigateway._
import com.github.yoshiyoshifujii.aws.lambda.{AWSLambda, FunctionName}

import scala.collection.JavaConverters._
import scala.util.Try

object Serverless {
  import sbtassembly.AssemblyPlugin.autoImport._
  import ServerlessPlugin.autoImport._

  def deployTask(key: TaskKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value

    val api = AWSApiGatewayRestApi(so.provider.region)
    val lambda = AWSLambda(so.provider.region)

    for {
      createRestApiResult <- api.create(
        name = (name in key).value,
        description = (description in key).?.value)

      putRestApiResult <- api.put(
        restApiId = createRestApiResult.getId,
        body = so.provider.swagger,
        mode = PutMode.Merge,
        failOnWarnings = None)

    } yield {

      lazy val authorize = AWSApiGatewayAuthorize(
        so.provider.region, createRestApiResult.getId
      )

      so.functions.foreach { function =>
        for {
          functionArn <- lambda.deploy(
            functionName = function.name,
            role = function.role,
            handler = function.handler,
            bucketName = so.provider.deploymentBucket,
            jar = function.filePath,
            description = function.description,
            timeout = Option(function.timeout),
            memorySize = Option(function.memorySize),
            environment = Option(function.environment),
            createAfter = arn => lambda.addPermission(arn)
          )

          publishVersionResult <- lambda.publishVersion(
            functionName = function.name,
            description = (version in deploy).?.value
          )

          aliasArn <- deployAlias(
            lambda = lambda,
            functionName = function.name,
            aliasName = so.provider.stage,
            functionVersion = Option(publishVersionResult.getVersion),
            description = None
          )

          _ <- Try {
            function.events.httpEventsForeach { httpEvent =>

              val method = AWSApiGatewayMethods(
                regionName = so.provider.region,
                restApiId = createRestApiResult.getId,
                path = httpEvent.path,
                httpMethod = httpEvent.method)

              httpEvent.authorizer.foreach { au =>
                authorize.deployAuthorizer(
                  name = au.name,
                  awsAccountId = so.provider.awsAccount,
                  lambdaName = function.name,
                  lambdaAlias = None,
                  identitySourceHeaderName = au.identitySourceHeaderName,
                  identityValidationExpression = au.identityValidationExpression,
                  authorizerResultTtlInSeconds = Option(au.resultTtlInSeconds)
                )
              }

              method.deploy(
                awsAccountId = so.provider.awsAccount,
                lambdaName = function.name,
                lambdaAlias = Option(s"$${stageVariables.env}${publishVersionResult.getVersion}"),
                requestTemplates = RequestTemplates(httpEvent.request.templateToSeq: _*),
                responseTemplates = httpEvent.response.statusCodes,
                withAuth(method)(
                  AWSApiGatewayAuthorize(so.provider.region, createRestApiResult.getId))(
                  httpEvent.authorizer.map(_.name))
              )

            }
          }

        } yield ???
      }
    }

  }

  private def deployAlias(lambda: AWSLambda,
                          functionName: FunctionName,
                          aliasName: String,
                          functionVersion: Option[String],
                          description: Option[String]) =
    for {
      aOp <- lambda.getAlias(functionName, aliasName)
      res <- aOp map { _ =>
        lambda.updateAlias(
          functionName = functionName,
          name = aliasName,
          functionVersion = functionVersion,
          description = description
        ).map(_.getAliasArn)
      } getOrElse {
        for {
          a <- lambda.createAlias(
            functionName = functionName,
            name = aliasName,
            functionVersion = functionVersion,
            description = description
          )
          _ <- lambda.addPermission(
            functionArn = a.getAliasArn
          )
        } yield a.getAliasArn
      }
    } yield res

  private lazy val withAuth =
    (method: AWSApiGatewayMethods) =>
      (authorize: AWSApiGatewayAuthorize) =>
        (authName: Option[String]) =>
          (resourceId: String) => Try {
            (authName map { name =>
              for {
                aOp <- authorize.getAuthorizer(name)
                r <- Try {
                  aOp map { a =>
                    method.updateMethod(
                      resourceId = resourceId,
                      "/authorizationType" -> "CUSTOM",
                      "/authorizerId" -> a.getId
                    ).get
                  } getOrElse(throw new RuntimeException(s"Custome Authorizer is nothing. $name"))
                }
              } yield r
            } getOrElse {
              method.updateMethod(
                resourceId = resourceId,
                "/authorizationType" -> "NONE"
              )
            }).get
            ()
          }
}
