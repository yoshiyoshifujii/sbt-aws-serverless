package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait RemoveBase extends KeysBase {

  def invoke: Try[Unit] = {
    for {
      _ <- swap {
        so.provider.restApiId.map { id =>
          api.delete(id)
        }
      }
      _ <- sequence {
        so.functions.map { f =>
          lambda.delete(f.name)
        }
      }
    } yield ()
  }

}

case class Remove(so: ServerlessOption) extends RemoveBase

