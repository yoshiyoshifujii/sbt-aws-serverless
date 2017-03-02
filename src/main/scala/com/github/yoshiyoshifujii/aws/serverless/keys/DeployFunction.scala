package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.{Function, ServerlessOption}

import scala.util.Try

trait DeployFunctionBase extends KeysBase {

  def invoke(function: Function,
             stage: Option[String]): Try[String] =
    lambda.deploy(
      functionName = function.name,
      role = function.role,
      handler = function.handler,
      bucketName = so.provider.deploymentBucket,
      jar = function.filePath,
      description = function.description,
      timeout = Option(function.timeout),
      memorySize = Option(function.memorySize),
      environment = stage.map(function.getEnvironment(_)).orElse(Some(function.environment)),
      createAfter = arn => lambda.addPermission(arn)
    ).map { functionArn =>
      println(s"Lambda deployed: $functionArn")
      functionArn
    }

}

case class DeployFunction(so: ServerlessOption) extends DeployFunctionBase

