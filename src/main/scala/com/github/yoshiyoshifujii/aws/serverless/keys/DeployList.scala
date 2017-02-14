package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait DeployListBase extends KeysBase {

  def invoke: Try[Unit] =
    for {
      _ <- swap {
        so.provider.restApiId map { id =>
          for {
            _ <- api.printStages(id)
            _ <- api.printDeployments(id)
            _ <- api.printAuthorizers(id)
          } yield ()
        }
      }
      _ <- sequence {
        so.functions.map { f =>
          for {
            _ <- lambda.printListVersionsByFunction(f.name)
            _ <- lambda.printListAliases(f.name)
          } yield ()
        }
      }
    } yield ()

}

case class DeployList(so: ServerlessOption) extends DeployListBase

