package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import serverless.ServerlessOption

trait ServerlessKeys {

  lazy val serverlessDeploy =
    inputKey[Unit]("The deploy task deploys the entire service.")

  lazy val serverlessDeployDev =
    inputKey[Unit]("Deploy the deployDev task in development mode.")

  lazy val serverlessDeployFunction =
    inputKey[Unit]("The deployFunc task deploys the AWS Lambda Function.")

  lazy val serverlessDeployList =
    inputKey[Unit]("The deployList task will list your recent deployments.")

  lazy val serverlessInvoke = inputKey[Unit]("Invokes deployed function.")

  lazy val serverlessInformation =
    taskKey[Unit]("Displays information about the deployed service.")

  lazy val serverlessRemove =
    taskKey[Unit]("The remove task will remove the deployed service.")

  lazy val serverlessRemoveStage = inputKey[Unit]("The removeStage task will remove the stage.")

  lazy val serverlessRemoveDeployment =
    inputKey[Unit]("The removeDeployment task will remove the API Gateway deployments.")

  lazy val serverlessDeployStream =
    inputKey[Unit]("The deployStream task deploys the AWS Stream Event.")

  lazy val serverlessClean = taskKey[Unit]("Clean up unnecessary deployments.")

  lazy val serverlessOption = taskKey[ServerlessOption]("")

  lazy val serverlessNoUploadMode =
    taskKey[Boolean]("If there is a Jar of the same name in S3, use it")

  lazy val serverlessFunctionsDeploy =
    inputKey[Unit]("The functionDeploy task deploys the AWS Lambda Function and API Gateway.")

  lazy val serverlessFunctionNames =
    taskKey[Seq[String]]("FunctionNames the functionDeploy task uses")
}

object ServerlessKeys extends ServerlessKeys
