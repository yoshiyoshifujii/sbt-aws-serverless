package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.PutMode
import com.github.yoshiyoshifujii.aws.Extension
import serverless.{Function, ServerlessOption}

import scala.util.Try

trait FunctionsDeployBase extends DeployBase {

  private def putRestApi(stage: String) =
    swap {
      for {
        ag        <- so.apiGateway
        restApiId <- ag.restApiId
      } yield
        for {
          getExportResult <- api.export(
            restApiId = restApiId,
            stageName = stage,
            extensions = Some(Set(Extension.Integrations, Extension.Authorizers))
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

  private def invokeFunctions(functions: Seq[Function], stage: String) = {
    sequence {
      functions.map { function =>
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
    }
  }

  def invokeFunctionsDeploy(functions: Seq[Function], stage: String): Try[Unit] =
    for {
      _ <- putRestApi(stage)
      _ <- invokeFunctions(functions, stage)
      _ <- createDeployment(stage)
    } yield ()
}

case class FunctionsDeploy(so: ServerlessOption,
                           name: String,
                           description: Option[String],
                           version: Option[String],
                           noUploadMode: Boolean)
    extends FunctionsDeployBase
