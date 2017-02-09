package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize
import com.amazonaws.services.apigateway.model.PutMode
import com.github.yoshiyoshifujii.aws.apigateway._
import com.github.yoshiyoshifujii.aws.lambda.{AWSLambda, FunctionName}

import scala.util.{Success, Try}

object Serverless {
  import sbtassembly.AssemblyPlugin.autoImport._
  import ServerlessPlugin.autoImport._


  def deployTask(key: TaskKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value

    val api = AWSApiGatewayRestApi(so.provider.region)
    val lambda = AWSLambda(so.provider.region)

    val a = for {
      restApiId <- {
        so.provider.restApiId map { id =>
          Try(id)
        } getOrElse {
          api.create(
            name = (name in key).value,
            description = (description in key).?.value
          ).map(_.getId)
        }
      }

      _ <- api.put(
        restApiId = restApiId,
        body = so.provider.swagger,
        mode = PutMode.Merge,
        failOnWarnings = None)

      _ <- sequence {
        lazy val authorize = AWSApiGatewayAuthorize(
          so.provider.region, restApiId
        )
        so.functions.map { function =>
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
              description = (version in deploy).?.value
            )

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
                  }
                  _ <- method.deploy(
                    awsAccountId = so.provider.awsAccount,
                    lambdaName = function.name,
                    lambdaAlias = Option(s"$${stageVariables.env}${publishVersionResult.getVersion}"),
                    requestTemplates = RequestTemplates(httpEvent.request.templateToSeq: _*),
                    responseTemplates = httpEvent.response.statusCodes,
                    withAuth(method)(
                      AWSApiGatewayAuthorize(so.provider.region, restApiId))(
                      httpEvent.authorizer.map(_.name))
                  )
                } yield ()
              }
            }

          } yield ()
        }
      }
    } yield ()

    a.get
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

  private def sequence[T](xs : Seq[Try[T]]) : Try[Seq[T]] = (Try(Seq[T]()) /: xs) {
    (a, b) => a flatMap (c => b map (d => c :+ d))
  }

  private def swap[T](optTry: Option[Try[T]]): Try[Option[T]] =
    optTry.map(_.map(Some.apply)).getOrElse(Success(None))

}
