package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait DeployListBase extends KeysBase {

}

case class DeployList(so: ServerlessOption) extends DeployListBase {

  def invoke: Try[Seq[Unit]] = sequence {
    so.functions.map { f =>
      for {
        _ <- lambda.printListVersionsByFunction(f.name)
        _ <- lambda.printListAliases(f.name)
      } yield ()
    }
  }

}

