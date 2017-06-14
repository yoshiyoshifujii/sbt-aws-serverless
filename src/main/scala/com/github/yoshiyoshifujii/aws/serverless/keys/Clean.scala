package com.github.yoshiyoshifujii.aws.serverless.keys

import java.nio.charset.StandardCharsets

import scala.collection.JavaConverters._
import scala.util.Try

trait CleanBase extends KeysBase {

  def invoke: Try[Unit] =
    swap {
      so.restApiId map { rid =>
        for {
          stages <- api.getStages(rid)
          e <- sequence {
            stages.getItem.asScala map { stage =>
              api.export(
                restApiId = rid,
                stageName = stage.getStageName
              ) map { export =>
                val json =
                  new String(export.getBody.array, StandardCharsets.UTF_8)
                """"uri" : "(.*)"""".r.findAllMatchIn(json).foreach { m =>
                  println(m.group(1))
                }

                export
              }
            }
          }
        } yield {}
      }
    }.map(_ => ())
}

case class Clean(so: serverless.ServerlessOption) extends CleanBase
