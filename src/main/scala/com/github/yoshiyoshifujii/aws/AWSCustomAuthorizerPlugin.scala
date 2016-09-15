package com.github.yoshiyoshifujii.aws

import com.github.yoshiyoshifujii.aws.apigateway.{AWSApiGatewayAuthorize, Uri}
import sbt._

import scala.util.Try

object AWSCustomAuthorizerPlugin extends AutoPlugin {

  object autoImport {
    lazy val getAuthorizers = taskKey[Unit]("")

    lazy val awsAuthorizerName = settingKey[String]("")
    lazy val awsIdentitySourceHeaderName = settingKey[String]("")
    lazy val awsIdentityValidationExpression = settingKey[String]("")
    lazy val awsAuthorizerResultTtlInSeconds = settingKey[Int]("")
  }

  import autoImport._
  import AWSServerlessPlugin.autoImport._
  import AWSApiGatewayPlugin.autoImport._

  override lazy val projectSettings = Seq(
    getAuthorizers := {
      val region = awsRegion.value
      AWSApiGatewayAuthorize(region).printAuthorizers(
        restApiId = awsApiGatewayRestApiId.value
      )
    },
    deployLambda := deployLambda.value,
    deploy := {
      val region = awsRegion.value
      val lambdaName = awsLambdaFunctionName.value
      val jar = sbtassembly.AssemblyKeys.assembly.value

      (for {
        lambdaArn <- Try(deployLambda.value)
        _ = {println(s"Lambda Deploy: $lambdaArn")}
        authorizerId <- AWSApiGatewayAuthorize(region).deployAuthorizer(
          restApiId = awsApiGatewayRestApiId.value,
          name = awsAuthorizerName.value,
          authorizerUri = Uri(
            region,
            awsAccountId.value,
            lambdaName,
            None
          ),
          identitySourceHeaderName = awsIdentitySourceHeaderName.value,
          identityValidationExpression = awsIdentityValidationExpression.?.value,
          authorizerResultTtlInSeconds = awsAuthorizerResultTtlInSeconds.?.value
        )
        _ = {println(s"API Gateway Authorizer Deploy: $authorizerId")}
      } yield jar).get
    },
    deployDev := deploy.value
  )
}
