package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import serverless.ServerlessOption

trait ServerlessKeys {

  lazy val deploy = inputKey[Unit]("The deploy task deploys the entire service.")

  lazy val deployCopy = inputKey[Unit]("The deployCopy task copy stage A to B.")

  lazy val deployDev = inputKey[Unit]("Deploy the deployDev task in development mode.")

  lazy val deployFunction = inputKey[Unit]("The deployFunc task deploys the AWS Lambda Function.")

  lazy val deployList = inputKey[Unit]("The deployList task will list your recent deployments.")

  lazy val invoke = inputKey[Unit]("Invokes deployed function.")

  lazy val logs = taskKey[Unit]("")

  lazy val information = taskKey[Unit]("Displays information about the deployed service.")

  lazy val remove = taskKey[Unit]("The remove task will remove the deployed service.")

  lazy val removeDeployment =
    inputKey[Unit]("The removeDeployment task will remove the API Gateway deployments.")

  lazy val serverlessOption = taskKey[ServerlessOption]("")

  lazy val serverlessNoUploadMode =
    taskKey[Boolean]("If there is a Jar of the same name in S3, use it")

  lazy val serverlessDeployStream =
    inputKey[Unit]("The deployStream task deploys the AWS Stream Event.")

  lazy val serverlessClean = inputKey[Unit]("")
}

object ServerlessKeys extends ServerlessKeys
