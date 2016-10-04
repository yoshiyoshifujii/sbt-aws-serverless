package com.github.yoshiyoshifujii.aws

import com.github.yoshiyoshifujii.aws.lambda.AWSLambda
import sbt._

import scala.util.Try

object AWSLambdaTriggerKinesisStreamPlugin extends AutoPlugin {

  object autoImport {
    lazy val listEventSourceMappings = inputKey[Unit]("")
    lazy val syncEventSourceMappings = inputKey[Unit]("")
    lazy val deleteEventSourceMappings = inputKey[Unit]("")

    lazy val eventSourceNames = settingKey[Seq[String]]("")
  }

  import autoImport._

  import AWSApiGatewayPlugin.autoImport._
  import AWSServerlessPlugin.autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override lazy val projectSettings = Seq(
    deployLambda := {
      val region = awsRegion.value
      val lambda = AWSLambda(region)
      lambda.deploy(
        functionName = awsLambdaFunctionName.value,
        role = awsLambdaRole.value,
        handler = awsLambdaHandler.value,
        bucketName = awsLambdaS3Bucket.value,
        jar = sbtassembly.AssemblyKeys.assembly.value,
        description = awsLambdaDescription.?.value,
        timeout = awsLambdaTimeout.?.value,
        memorySize = awsLambdaMemorySize.?.value).get
    },
    deploy := {
      val region = awsRegion.value
      val lambdaName = awsLambdaFunctionName.value
      val jar = sbtassembly.AssemblyKeys.assembly.value
      val description = awsLambdaDeployDescription.?.value
      val lambda = AWSLambda(region)

      lazy val publish = lambda.publishVersion(
        functionName = awsLambdaFunctionName.value,
        description = description
      )

      lazy val createAliases = (v: String) => Try {
        awsLambdaAliasNames.value map { name =>
          (for {
            a <- lambda.createAlias(
              functionName = lambdaName,
              name = s"$name$v",
              functionVersion = Some(v),
              description = description
            )
          } yield a).get
        }
      }

      (for {
        lambdaArn <- Try(deployLambda.value)
        _ = {println(s"Lambda Deploy: $lambdaArn")}
        v <- publish
        _ = {println(s"Publish Lambda: ${v.getFunctionArn}")}
        cars <- createAliases(v.getVersion)
        _ = {cars.foreach(c => println(s"Create Alias: ${c.getAliasArn}"))}
      } yield jar).get
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
    listEventSourceMappings := {
      import complete.DefaultParsers._

      val region = awsRegion.value
      val functionName = awsLambdaFunctionName.value

      val lambda = AWSLambda(region)

      val arn = spaceDelimited("<arg>").parsed match {
        case Seq(stageName) =>
          lambda.generateLambdaArn(awsAccountId.value)(functionName)(Some(stageName))
        case _ =>
          lambda.generateLambdaArn(awsAccountId.value)(functionName)(None)
      }
      lambda.printEventSourceMappings(arn)
    },
    syncEventSourceMappings := {
      import complete.DefaultParsers._

      val region = awsRegion.value
      val awsAccount = awsAccountId.value
      val functionName = awsLambdaFunctionName.value

      val lambda = AWSLambda(region)

      val (functionArn, generateEventSourceName) = spaceDelimited("<arg>").parsed match {
        case Seq(stageName) =>
          (lambda.generateLambdaArn(awsAccountId.value)(functionName)(Some(stageName)),
            (eventSourceName: String) => s"$eventSourceName-$stageName")
        case _ =>
          (lambda.generateLambdaArn(awsAccountId.value)(functionName)(None),
            (eventSourceName: String) => eventSourceName)
      }

      for {
        l <- lambda.deleteEventSourceMappings(functionArn)
        c <- Try {
          eventSourceNames.value.map { name =>
            val eventSourceArn = kinesis.generateKinesisStreamArn(region)(awsAccount)(generateEventSourceName(name))
            lambda.createEventSourceMapping(functionArn, eventSourceArn).get
          }
        }
      } yield c

    },
    deleteEventSourceMappings := {
      import complete.DefaultParsers._

      val region = awsRegion.value
      val functionName = awsLambdaFunctionName.value

      val lambda = AWSLambda(region)

      val functionArn = spaceDelimited("<arg>").parsed match {
        case Seq(stageName) =>
          lambda.generateLambdaArn(awsAccountId.value)(functionName)(Some(stageName))
        case _ =>
          lambda.generateLambdaArn(awsAccountId.value)(functionName)(None)
      }

      lambda.deleteEventSourceMappings(functionArn).get
    },
    unDeploy := ? {
      val region = awsRegion.value
      for {
        l <- AWSLambda(region).delete(awsLambdaFunctionName.value)
      } yield l
    }
  )

}
