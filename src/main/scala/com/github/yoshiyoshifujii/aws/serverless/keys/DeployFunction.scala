package com.github.yoshiyoshifujii.aws.serverless.keys

import com.github.yoshiyoshifujii.aws.s3.AWSS3
import sbt.File
import serverless.{Function, ServerlessOption}

import scala.util.Try

trait DeployFunctionBase extends KeysBase {

  val noUploadMode: Boolean

  private lazy val s3 = new AWSS3(so.provider.region)

  def invoke(function: Function, stage: String): Try[String] = {

    val putS3: (String, File) => Try[String] =
      if (noUploadMode) s3.putIfDoesNotObjectExist else s3.put

    for {
      key <- putS3(
        so.provider.deploymentBucket,
        function.filePath
      )
      arn <- lambda.deploy(
        functionName = function.nameWith(stage),
        role = function.role,
        handler = function.handler,
        bucketName = so.provider.deploymentBucket,
        key = key,
        description = function.description,
        timeout = Option(function.timeout),
        memorySize = Option(function.memorySize),
        environment = Option(function.getEnvironment(stage)),
        reservedConcurrentExecutions = function.reservedConcurrentExecutions,
        tags = if (function.tags.nonEmpty) Some(function.tags) else None,
        tracingMode = function.tracing.map(_.value),
        createAfter = arn => lambda.addPermission(arn)
      )
    } yield {
      println(s"Lambda deployed: $arn")
      arn
    }
  }

}

case class DeployFunction(so: ServerlessOption, noUploadMode: Boolean) extends DeployFunctionBase
