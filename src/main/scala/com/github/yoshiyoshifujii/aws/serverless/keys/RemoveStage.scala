package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.GetStageResult
import com.github.yoshiyoshifujii.aws.apigateway.RestApiId
import serverless.ServerlessOption

import scala.collection.JavaConverters._
import scala.util.Try

trait RemoveStageBase extends KeysBase {

  def invoke(stage: String): Try[Unit] =
    swap {
      so.restApiId map { restApiId =>
        for {
          stageOpt     <- api.getStage(restApiId, stage)
        } yield ()
      }
    } map (_ => ())

}

case class RemoveStage(so: ServerlessOption) extends RemoveStageBase
