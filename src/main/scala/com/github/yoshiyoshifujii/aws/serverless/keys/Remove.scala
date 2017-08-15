package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait RemoveBase extends KeysBase {

  def invoke: Try[Unit] = {
    for {
      _ <- swap {
        for {
          ag <- so.apiGateway
          id <- ag.restApiId
        } yield
          for {
            _ <- api.delete(id)
            _ <- ag.deleteRestApiId()
          } yield ()
      }
    } yield ()
  }

}

case class Remove(so: ServerlessOption) extends RemoveBase
