package com.github.yoshiyoshifujii.aws

import com.github.yoshiyoshifujii.aws.lambda.AWSLambda
import sbt._

object AWSLambdaTriggerKinesisStreamPlugin extends AutoPlugin {

  object autoImport {

  }

  import AWSApiGatewayPlugin.autoImport._
  import AWSServerlessPlugin.autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override lazy val projectSettings = Seq(
    deploy := {
      val region = awsRegion.value
      val jar = sbtassembly.AssemblyKeys.assembly.value
      val lambda = AWSLambda(region)
      lambda.deploy(
        functionName = awsLambdaFunctionName.value,
        role = awsLambdaRole.value,
        handler = awsLambdaHandler.value,
        bucketName = awsLambdaS3Bucket.value,
        jar = jar,
        description = awsLambdaDescription.?.value,
        timeout = awsLambdaTimeout.?.value,
        memorySize = awsLambdaMemorySize.?.value
      ).get
      jar
    },
    listLambdaVersions := {
      val region = awsRegion.value
      AWSLambda(region)
        .printListVersionsByFunction(awsLambdaFunctionName.value)
        .get
    },
    listLambdaAliases := {
      val region = awsRegion.value
      AWSLambda(region)
        .printListAliases(awsLambdaFunctionName.value)
        .get
    },
    unDeploy := ? {
      val region = awsRegion.value
      for {
        l <- AWSLambda(region).delete(awsLambdaFunctionName.value)
      } yield l
    }
  )

}
