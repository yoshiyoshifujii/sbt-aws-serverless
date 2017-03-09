package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait RemoveDeploymentBase extends KeysBase {

  def invoke(deploymentId: String): Try[Unit] = {
    for {
      _ <- swap {
        for {
          ag <- so.apiGateway
          id <- ag.restApiId
        } yield api.deleteDeployment(id, deploymentId)
      }
    } yield ()
  }

}

case class RemoveDeployment(so: ServerlessOption) extends RemoveDeploymentBase

