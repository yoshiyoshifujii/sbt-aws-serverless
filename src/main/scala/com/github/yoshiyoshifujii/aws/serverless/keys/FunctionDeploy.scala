package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.PutMode
import serverless.{Function, ServerlessOption}

import scala.util.Try

trait FunctionDeployBase extends DeployBase {

  private def putRestApi(stage: String) =
    swap {
      for {
        ag        <- so.apiGateway
        restApiId <- ag.restApiId
      } yield
        for {
          getExportResult <- api.export(
            restApiId = restApiId,
            stageName = stage
          )
          putRestApiResult <- api.put(
            restApiId = restApiId,
            body = getExportResult.getBody,
            mode = PutMode.Overwrite,
            failOnWarnings = None
          )
          _ = println(s"API Gateway put: ${putRestApiResult.getId}")
        } yield putRestApiResult
    }

  def invokeFunction(function: Function, stage: String) = {
    for {
      _                <- invoke(function, stage)
      publishedVersion <- publishVersion(function, stage)
      _ <- deployEvents(
        stage = stage,
        function = function,
        publishedVersion = publishedVersion
      )
    } yield ()
  }

  def invokeFunctionDeploy(function: Function, stage: String): Try[Unit] =
    for {
      _ <- putRestApi(stage)
      _ <- invokeFunction(function, stage)
      _ <- createDeployment(stage)
    } yield ()
}

case class FunctionDeploy(so: ServerlessOption,
                          name: String,
                          description: Option[String],
                          version: Option[String],
                          noUploadMode: Boolean)
    extends FunctionDeployBase
