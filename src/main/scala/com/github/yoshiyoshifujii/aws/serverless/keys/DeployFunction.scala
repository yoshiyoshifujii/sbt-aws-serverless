package com.github.yoshiyoshifujii.aws.serverless.keys

import com.github.yoshiyoshifujii.aws.lambda.AWSLambda
import serverless.{Function, ServerlessOption}

import scala.util.Try

trait DeployFunctionBase {

  val so: ServerlessOption
  lazy protected val lambda = AWSLambda(so.provider.region)

  def deployFunction(function: Function): Try[String] =
    lambda.deploy(
      functionName = function.name,
      role = function.role,
      handler = function.handler,
      bucketName = so.provider.deploymentBucket,
      jar = function.filePath,
      description = function.description,
      timeout = Option(function.timeout),
      memorySize = Option(function.memorySize),
      environment = Option(function.environment),
      createAfter = arn => lambda.addPermission(arn)
    ).map { functionArn =>
      println(s"Lambda deployed: $functionArn")
      functionArn
    }

}

case class DeployFunction(so: ServerlessOption) extends DeployFunctionBase

