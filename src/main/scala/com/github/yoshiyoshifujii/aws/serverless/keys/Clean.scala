package com.github.yoshiyoshifujii.aws.serverless.keys

import scala.util.Try

trait CleanBase extends KeysBase {

  def invoke(stageName: String): Try[Unit] =
    swap {
      so.restApiId map { rid =>
        for {
          e <- api.export(
            restApiId = rid,
            stageName = stageName
          )
        } yield {
          println(e.getBody)
        }
      }
    }.map(_ => ())
}

case class Clean(so: serverless.ServerlessOption) extends CleanBase
