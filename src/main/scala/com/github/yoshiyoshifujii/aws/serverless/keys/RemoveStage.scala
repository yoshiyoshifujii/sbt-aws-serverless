package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait RemoveStageBase extends KeysBase {

  def invoke(stage: String): Try[Unit] =
    for {
      _ <- swap {
        so.restApiId map { restApiId =>
          for {
            stageOpt <- api.getStage(restApiId, stage)
            _ <- swap {
              stageOpt map { _ =>
                api.deleteStage(restApiId, stage)
              }
            }
          } yield ()
        }
      }
      _ <- sequence {
        so.functions.map { f =>
          lambda.delete(f.nameWith(stage))
        }
      }
    } yield ()

}

case class RemoveStage(so: ServerlessOption) extends RemoveStageBase
