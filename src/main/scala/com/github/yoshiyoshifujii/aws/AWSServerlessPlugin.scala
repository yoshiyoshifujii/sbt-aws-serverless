package com.github.yoshiyoshifujii.aws

import com.github.yoshiyoshifujii.aws.apigateway._
import com.github.yoshiyoshifujii.aws.lambda.AWSLambda
import sbt._

import scala.util.Try

object AWSServerlessPlugin extends AutoPlugin {

  object autoImport {
    lazy val deploy = taskKey[File]("")
    lazy val deployDev = taskKey[File]("")
    lazy val listLambdaVersions = taskKey[Unit]("")
    lazy val listLambdaAliases = taskKey[Unit]("")

    lazy val awsLambdaFunctionName = settingKey[String]("")
    lazy val awsLambdaDescription = settingKey[String]("")
    lazy val awsLambdaHandler = settingKey[String]("")
    lazy val awsLambdaRole = settingKey[String]("")
    lazy val awsLambdaTimeout = settingKey[Int]("")
    lazy val awsLambdaMemorySize = settingKey[Int]("")
    lazy val awsLambdaS3Bucket = settingKey[String]("")
    lazy val awsLambdaDeployDescription = settingKey[String]("")
    lazy val awsLambdaAliasNames = settingKey[Seq[String]]("")

    lazy val awsApiGatewayResourcePath = settingKey[String]("")
    lazy val awsApiGatewayResourceHttpMethod = settingKey[String]("")
    lazy val awsApiGatewayResourceUriLambdaAlias = settingKey[String]("")
    lazy val awsApiGatewayIntegrationRequestTemplates = settingKey[Seq[(String, String)]]("")
    lazy val awsApiGatewayIntegrationResponseTemplates = settingKey[ResponseTemplates]("")
  }

  import autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override lazy val projectSettings = Seq(
    deploy := {
      val region = AWSApiGatewayPlugin.autoImport.awsRegion.value
      val lambdaName = awsLambdaFunctionName.value
      val jar = sbtassembly.AssemblyKeys.assembly.value
      val lambda = new AWSLambda(region)

      lazy val deployLambda = lambda.deploy(
        functionName = lambdaName,
        role = awsLambdaRole.value,
        handler = awsLambdaHandler.value,
        bucketName = awsLambdaS3Bucket.value,
        jar = jar,
        description = awsLambdaDescription.?.value,
        timeout = awsLambdaTimeout.?.value,
        memorySize = awsLambdaMemorySize.?.value
      )

      lazy val publish = lambda.publishVersion(
        functionName = awsLambdaFunctionName.value,
        description = awsLambdaDeployDescription.?.value
      )

      lazy val createAliases = (v: String) => Try {
        awsLambdaAliasNames.value map { name =>
          (for {
            a <- lambda.createAlias(
              functionName = lambdaName,
              name = s"$name$v",
              functionVersion = Some(v),
              description = None
            )
            p <- lambda.addPermission(
              functionName = a.getAliasArn
            )
          } yield a).get
        }
      }

      lazy val deployResource = (v: String) => new AWSApiGatewayMethods(region).deploy(
        restApiId = AWSApiGatewayPlugin.autoImport.awsApiGatewayRestApiId.value,
        path = awsApiGatewayResourcePath.value,
        httpMethod = awsApiGatewayResourceHttpMethod.value,
        uri = Uri(
          region,
          AWSApiGatewayPlugin.autoImport.awsAccountId.value,
          lambdaName,
          awsApiGatewayResourceUriLambdaAlias.?.value.map(a => s"$a$v")
        ),
        requestTemplates = RequestTemplates(awsApiGatewayIntegrationRequestTemplates.value: _*),
        responseTemplates = awsApiGatewayIntegrationResponseTemplates.value
      )

      (for {
        lambdaArn <- deployLambda
        _ = {println(s"Lambda Deploy: $lambdaArn")}
        v <- publish
        _ = {println(s"Publish Lambda: ${v.getFunctionArn}")}
        cars <- createAliases(v.getVersion)
        _ = {cars.foreach(c => println(s"Create Alias: ${c.getAliasArn}"))}
        resource <- deployResource(v.getVersion)
        _ = {println(s"Api Gateway Deploy")}
      } yield jar).get
    },
    deployDev := {
      val region = AWSApiGatewayPlugin.autoImport.awsRegion.value
      val lambdaName = awsLambdaFunctionName.value
      val jar = sbtassembly.AssemblyKeys.assembly.value
      val lambda = new AWSLambda(region)

      lazy val deployLambda = lambda.deploy(
        functionName = lambdaName,
        role = awsLambdaRole.value,
        handler = awsLambdaHandler.value,
        bucketName = awsLambdaS3Bucket.value,
        jar = jar,
        description = awsLambdaDescription.?.value,
        timeout = awsLambdaTimeout.?.value,
        memorySize = awsLambdaMemorySize.?.value
      )

      lazy val createAlias = Try {
        (for {
          a <- lambda.createAlias(
            functionName = lambdaName,
            name = s"dev",
            functionVersion = Some("$LATEST"),
            description = None
          )
          p <- lambda.addPermission(
            functionName = a.getAliasArn
          )
        } yield a).get
      }

      lazy val deployResource = new AWSApiGatewayMethods(region).deploy(
        restApiId = AWSApiGatewayPlugin.autoImport.awsApiGatewayRestApiId.value,
        path = awsApiGatewayResourcePath.value,
        httpMethod = awsApiGatewayResourceHttpMethod.value,
        uri = Uri(
          region,
          AWSApiGatewayPlugin.autoImport.awsAccountId.value,
          lambdaName,
          awsApiGatewayResourceUriLambdaAlias.?.value
        ),
        requestTemplates = RequestTemplates(awsApiGatewayIntegrationRequestTemplates.value: _*),
        responseTemplates = awsApiGatewayIntegrationResponseTemplates.value
      )

      (for {
        lambdaArn <- deployLambda
        _ = {println(s"Lambda Deploy: $lambdaArn")}
        v <- createAlias
        _ = {println(s"Create Alias: ${v.getAliasArn}")}
        resource <- deployResource
        _ = {println(s"Api Gateway Deploy")}
      } yield jar).get
    },
    listLambdaVersions := {
      val region = AWSApiGatewayPlugin.autoImport.awsRegion.value
      new AWSLambda(region)
        .printListVersionsByFunction(awsLambdaFunctionName.value)
        .get
    },
    listLambdaAliases := {
      val region = AWSApiGatewayPlugin.autoImport.awsRegion.value
      new AWSLambda(region)
        .printListAliases(awsLambdaFunctionName.value)
        .get
    }
  )
}
