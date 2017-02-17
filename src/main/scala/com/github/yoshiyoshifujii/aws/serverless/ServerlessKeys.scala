package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import serverless.ServerlessOption

trait ServerlessKeys {

  lazy val deploy         = inputKey[Unit]("The deploy task deploys the entire service.")

  lazy val deployDev      = inputKey[Unit]("Deploy the deployDev task in development mode.")

  lazy val deployFunction = inputKey[Unit]("The deployFunc task deploys the AWS Lambda Function.")

  lazy val deployList     = taskKey[Unit]("The deployList task will list your recent deployments.")

  lazy val invoke         = inputKey[Unit]("Invokes deployed function.")

  lazy val logs           = taskKey[Unit]("")

  lazy val information    = taskKey[Unit]("Displays information about the deployed service.")

  lazy val remove         = taskKey[Unit]("The remove task will remove the deployed service.")

  lazy val serverlessOption = taskKey[ServerlessOption]("")
}

object ServerlessKeys extends ServerlessKeys

