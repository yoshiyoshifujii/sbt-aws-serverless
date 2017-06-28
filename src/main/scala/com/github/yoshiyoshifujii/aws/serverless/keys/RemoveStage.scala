package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.{GetStageResult, Stage}
import com.github.yoshiyoshifujii.aws.apigateway.RestApiId
import serverless.ServerlessOption

import scala.collection.JavaConverters._
import scala.util.Try

trait RemoveStageBase extends KeysBase {

  private def getFunctionArns(restApiId: RestApiId, stageOpt: Option[GetStageResult]) =
    swap {
      stageOpt map { stage =>
        val stageName      = stage.getStageName
        val stageVariables = stage.getVariables.asScala.toMap
        api.exportFunctionArns(restApiId, stageName, stageVariables)
      }
    }

  def invoke(stage: String): Try[Unit] =
    swap {
      so.restApiId map { restApiId =>
        for {
          stageOpt     <- api.getStage(restApiId, stage)
          functionArns <- getFunctionArns(restApiId, stageOpt)
        } yield ()
      }
    } map (_ => ())

}

case class RemoveStage(so: ServerlessOption) extends RemoveStageBase
