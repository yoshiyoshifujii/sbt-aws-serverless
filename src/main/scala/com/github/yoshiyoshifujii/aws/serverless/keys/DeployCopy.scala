package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.GetStageResult
import com.github.yoshiyoshifujii.aws.apigateway.RestApiId
import serverless.{Function => ServerlessFunction, _}

import scala.collection.JavaConverters._
import scala.util.Try

trait DeployCopyBase extends DeployAlias with DeployStream {

  private def getFromStage(from: String, restApiId: RestApiId) =
    for {
      fromStageOpt <- api.getStage(restApiId, from)
      fromStage <- Try(fromStageOpt.getOrElse(throw new RuntimeException(s"from stage is not exists. $from")))
    } yield fromStage

  private def getPublishedVersion(from: String, function: ServerlessFunction): Try[Option[String]] = {
    for {
      lv <- lambda.listVersionsByFunction(function.name)
    } yield lv.getVersions.asScala.lastOption.map(_.getVersion)
  }

  private def deployEvents(restApiId: RestApiId,
                           stage: String,
                           function: FunctionBase,
                           publishedVersion: PublishedVersion) =
    for {
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

  private def copyFunctions(from: String, to: String, restApiId: String) =
    sequence {
      so.functions.map {
        case (function: ServerlessFunction) =>
          for {
            publishedVersion <- getPublishedVersion(from, function)
            _ <- Try(publishedVersion.getOrElse(throw new RuntimeException(s"from not published. [$from] [${function.name}]")))
            _ <- deployAlias(
              stage = to,
              function = function,
              publishedVersion = publishedVersion
            )
            _ <- deployEvents(
              restApiId = restApiId,
              stage = to,
              function = function,
              publishedVersion = publishedVersion
            )
          } yield ()
        case (ndlFunction: NotDeployLambdaFunction) =>
          for {
            _ <- deployEvents(
              restApiId = restApiId,
              stage = to,
              function = ndlFunction,
              publishedVersion = ndlFunction.publishedVersion
            )
          } yield ()
      }
    }

  private def copyStage(fromStage: GetStageResult, to: String, restApiId: String) =
    for {
      toDeploymentId <- api.createOrUpdateStage(
        restApiId = restApiId,
        stageName = to,
        deploymentId = fromStage.getDeploymentId,
        description = None,
        variables = so.provider.getStageVariables(to)
      )
      _ = { println(s"Stage: $to($toDeploymentId).") }
    } yield ()

  def invoke(from: String, to: String): Try[Option[Unit]] = {
    swap {
      for {
        restApiId <- so.provider.restApiId
      } yield for {
        fromStage <- getFromStage(from, restApiId)
        _ <- copyFunctions(from, to, restApiId)
        _ <- copyStage(fromStage, to, restApiId)
      } yield ()
    }
  }

}

case class DeployCopy(so: ServerlessOption,
                      version: Option[String]) extends DeployCopyBase

