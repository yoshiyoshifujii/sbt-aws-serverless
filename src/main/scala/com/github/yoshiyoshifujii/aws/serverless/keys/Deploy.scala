package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.PutMode
import serverless.{Function => ServerlessFunction, _}

import scala.util.Try

trait DeployBase
    extends DeployFunctionBase
    with DeployAlias
    with DeployResource
    with DeployAuthorizer
    with DeployStreamBase {
  val name: String
  val description: Option[String]

  private def getOrCreateRestApi =
    swap {
      so.apiGateway map { ag =>
        for {
          restApiId <- ag.restApiId map { id =>
            Try(id)
          } getOrElse {
            for {
              c <- api.create(
                name = name,
                description = description
              )
              _ <- ag.writeRestApiId(c.getId)
            } yield c.getId
          }
          _ = { println(s"API Gateway created: $restApiId") }
        } yield restApiId
      }
    }

  private def putRestApi() =
    swap {
      for {
        ag        <- so.apiGateway
        restApiId <- ag.restApiId
      } yield
        for {
          putRestApiResult <- api.put(
            restApiId = restApiId,
            body = ag.swagger,
            mode = PutMode.Merge,
            failOnWarnings = None
          )
          _ = { println(s"API Gateway put: ${putRestApiResult.getId}") }
        } yield putRestApiResult
    }

  private def createDeployment(stage: String) =
    swap {
      for {
        ag        <- so.apiGateway
        restApiId <- ag.restApiId
      } yield
        for {
          createDeploymentResult <- api.createDeployment(
            restApiId = restApiId,
            stageName = stage,
            stageDescription = None,
            description = version,
            variables = ag.getStageVariables(so.provider.region, stage)
          )
          _ = { println(s"Create Deployment: ${createDeploymentResult.toString}") }
        } yield createDeploymentResult
    }

  protected def publishVersion(function: ServerlessFunction) =
    for {
      publishVersionResult <- lambda.publishVersion(
        functionName = function.name,
        description = version
      )
      _ = { println(s"Lambda published: ${publishVersionResult.getFunctionArn}") }
    } yield Option(publishVersionResult.getVersion)

  private def deployEvents(stage: String,
                           function: FunctionBase,
                           publishedVersion: PublishedVersion) =
    for {
      _ <- swap {
        so.restApiId map { restApiId =>
          sequence {
            function.events.httpEventsMap { httpEvent =>
              deployResource(
                restApiId = restApiId,
                function = function,
                lambdaAlias =
                  httpEvent.uriLambdaAlias.map(a => generateLambdaAlias(a, publishedVersion)),
                httpEvent = httpEvent
              )
            }
          }
        }
      }
      _ <- swap {
        so.restApiId map { restApiId =>
          sequence {
            function.events.authorizeEventsMap { authorizeEvent =>
              deployAuthorizer(
                restApiId = restApiId,
                function = function,
                lambdaAlias = authorizeEvent.uriLambdaAlias,
                authorizeEvent = authorizeEvent
              )
            }
          }
        }
      }
      _ <- sequence {
        function.events.streamEventsMap { streamEvent =>
          deployStream(
            stage = stage,
            function = function,
            streamEvent = streamEvent
          )
        }
      }
    } yield ()

  private def invokeFunctions(stage: String) =
    sequence {
      so.functions.map {
        case (function: ServerlessFunction) =>
          for {
            _                <- invoke(function, Some(stage))
            publishedVersion <- publishVersion(function)
            _ <- deployAlias(
              stage = stage,
              function = function,
              publishedVersion = publishedVersion
            )
            _ <- deployEvents(
              stage = stage,
              function = function,
              publishedVersion = publishedVersion
            )
          } yield ()
        case (ndlFunction: NotDeployLambdaFunction) =>
          for {
            _ <- deployEvents(
              stage = stage,
              function = ndlFunction,
              publishedVersion = ndlFunction.publishedVersion
            )
          } yield ()
      }
    }

  private def validateFunctions: Try[Unit] = Try {
    val notExistsFunction = so.functions.notExistsFilePathFunctions
    if (notExistsFunction.nonEmpty) {
      throw new RuntimeException(
        s"Not Exists Function file path.\n${notExistsFunction.map(_.name).mkString("\n")}")
    }
  }

  def invoke(stage: String): Try[Unit] =
    for {
      _ <- validateFunctions
      a <- getOrCreateRestApi
      _ <- putRestApi
      _ <- invokeFunctions(stage)
      _ <- createDeployment(stage)
    } yield ()

}

case class Deploy(so: ServerlessOption,
                  name: String,
                  description: Option[String],
                  version: Option[String],
                  noUploadMode: Boolean)
    extends DeployBase
