package com.github.yoshiyoshifujii.aws

import java.io.ByteArrayOutputStream

import com.github.yoshiyoshifujii.aws.apigateway._
import com.github.yoshiyoshifujii.aws.lambda.AWSLambda
import org.apache.http.client.methods.CloseableHttpResponse
import sbt._

import scala.util.Try

object AWSServerlessPlugin extends AutoPlugin {

  object autoImport {
    lazy val deployLambda = taskKey[String]("")
    lazy val deploy = taskKey[File]("")
    lazy val deployDev = taskKey[File]("")
    lazy val deployResource = taskKey[Unit]("")
    lazy val listLambdaVersions = taskKey[Unit]("")
    lazy val listLambdaAliases = taskKey[Unit]("")
    lazy val unDeploy = taskKey[Unit]("")
    lazy val testMethod = inputKey[Unit]("")

    lazy val awsLambdaFunctionName = settingKey[String]("")
    lazy val awsLambdaDescription = settingKey[String]("")
    lazy val awsLambdaHandler = settingKey[String]("")
    lazy val awsLambdaRole = settingKey[String]("")
    lazy val awsLambdaTimeout = settingKey[Int]("")
    lazy val awsLambdaMemorySize = settingKey[Int]("")
    lazy val awsLambdaEnvironment = settingKey[Map[String, String]]("")
    lazy val awsLambdaS3Bucket = settingKey[String]("")
    lazy val awsLambdaDeployDescription = settingKey[String]("")
    lazy val awsLambdaAliasNames = settingKey[Seq[String]]("")

    lazy val awsApiGatewayResourcePath = settingKey[String]("")
    lazy val awsApiGatewayResourceHttpMethod = settingKey[String]("")
    lazy val awsApiGatewayResourceUriLambdaAlias = settingKey[String]("")
    lazy val awsApiGatewayIntegrationRequestTemplates = settingKey[Seq[(String, String)]]("")
    lazy val awsApiGatewayIntegrationResponseTemplates = settingKey[ResponseTemplates]("")

    lazy val awsMethodAuthorizerName = settingKey[String]("")

    lazy val awsTestHeaders = settingKey[Seq[(String, String)]]("")
    lazy val awsTestParameters = settingKey[Seq[(String, String)]]("")
    lazy val awsTestPathWithQuerys = settingKey[Seq[(String, String)]]("")
    lazy val awsTestBody = settingKey[String]("")
    lazy val awsTestSuccessStatusCode = settingKey[Int]("")
  }

  import autoImport._
  import AWSApiGatewayPlugin.autoImport._

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
        memorySize = awsLambdaMemorySize.?.value,
        environment = awsLambdaEnvironment.?.value,
        createAfter = arn => lambda.addPermission(arn)).get
    },
    deploy := {
      val region = awsRegion.value
      val lambdaName = awsLambdaFunctionName.value
      val jar = sbtassembly.AssemblyKeys.assembly.value
      val restApiId = awsApiGatewayRestApiId.value
      val description = awsLambdaDeployDescription.?.value

      val lambda = AWSLambda(region)
      val method = AWSApiGatewayMethods(
        regionName = region,
        restApiId = restApiId,
        path = awsApiGatewayResourcePath.value,
        httpMethod = awsApiGatewayResourceHttpMethod.value
      )

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
            p <- lambda.addPermission(
              functionArn = a.getAliasArn
            )
          } yield a).get
        }
      }

      lazy val deployResource = (v: String) =>
        method.deploy(
          awsAccountId = awsAccountId.value,
          lambdaName = lambdaName,
          lambdaAlias = awsApiGatewayResourceUriLambdaAlias.?.value.map(a => s"$a$v"),
          requestTemplates = RequestTemplates(awsApiGatewayIntegrationRequestTemplates.value: _*),
          responseTemplates = awsApiGatewayIntegrationResponseTemplates.value,
          withAuth(method)(AWSApiGatewayAuthorize(region, restApiId))(awsMethodAuthorizerName.?.value)
        )

      (for {
        lambdaArn <- Try(deployLambda.value)
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
      val region = awsRegion.value
      val lambdaName = awsLambdaFunctionName.value
      val jar = sbtassembly.AssemblyKeys.assembly.value
      val lambda = AWSLambda(region)

      lazy val createAliasIfNotExists = Try {
        (for {
          aOp <- lambda.getAlias(lambdaName, "dev")
          res <- aOp map { a =>
            Try(a.getAliasArn)
          } getOrElse {
            for {
              a <- lambda.createAlias(
                functionName = lambdaName,
                name = s"dev",
                functionVersion = Some("$LATEST"),
                description = None
              )
              p <- lambda.addPermission(
                functionArn = a.getAliasArn
              )
            } yield a.getAliasArn
          }
        } yield res).get
      }

      (for {
        lambdaArn <- Try(deployLambda.value)
        _ = {println(s"Lambda Deploy: $lambdaArn")}
        v <- createAliasIfNotExists
        _ = {println(s"Create Alias: $v")}
        resource <- Try(deployResource.value)
        _ = {println(s"Api Gateway Deploy")}
      } yield jar).get
    },
    deployResource := {
      val region = awsRegion.value
      val restApiId = awsApiGatewayRestApiId.value
      val lambdaName = awsLambdaFunctionName.value
      val method = AWSApiGatewayMethods(
        regionName = region,
        restApiId = restApiId,
        path = awsApiGatewayResourcePath.value,
        httpMethod = awsApiGatewayResourceHttpMethod.value
      )

      method.deploy(
        awsAccountId = awsAccountId.value,
        lambdaName = lambdaName,
        lambdaAlias = awsApiGatewayResourceUriLambdaAlias.?.value,
        requestTemplates = RequestTemplates(awsApiGatewayIntegrationRequestTemplates.value: _*),
        responseTemplates = awsApiGatewayIntegrationResponseTemplates.value,
        withAuth(method)(AWSApiGatewayAuthorize(region, restApiId))(awsMethodAuthorizerName.?.value)
      ).get
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
      val lambdaName = awsLambdaFunctionName.value
      val lambda = AWSLambda(region)
      val method = AWSApiGatewayMethods(
        regionName = region,
        restApiId = awsApiGatewayRestApiId.value,
        path = awsApiGatewayResourcePath.value,
        httpMethod = awsApiGatewayResourceHttpMethod.value
      )

      (for {
        _ <- lambda.delete(lambdaName)
        _ = {println(s"Lambda deleted: $lambdaName")}
        resource <- method.upDeploy()
        _ = {println(s"Resouce deleted")}
      } yield unit).get
    },
    testMethod := {
      import complete.DefaultParsers._
      val region = awsRegion.value
      val restApiId = awsApiGatewayRestApiId.value
      val path = awsApiGatewayResourcePath.value
      spaceDelimited("<stageName>").parsed match {
        case Seq(stageName) =>
          import com.github.yoshiyoshifujii.aws.http._
          val url = generateUrl(
            region = region,
            restApiId = restApiId,
            stageName = stageName,
            path = path,
            pathWithQuerys = awsTestPathWithQuerys.?.value.getOrElse(Seq()))
          (for {
            response <- request(
              url = url,
              method = awsApiGatewayResourceHttpMethod.value,
              headers = awsTestHeaders.?.value.getOrElse(Seq()),
              parameters = awsTestParameters.?.value.getOrElse(Seq()),
              body = awsTestBody.?.value.map(_.getBytes("utf-8"))
            )
          } yield {
            val statusCode = response.getStatusLine.getStatusCode
            if (Some(statusCode) == awsTestSuccessStatusCode.?.value)
              printResponse(url)(response)
            else
              sys.error(s"test method failed. $statusCode")
          }).get
        case _ =>
          sys.error("Error testMethod. useage: testMethod <stageName>")
      }
    }
  )

  private lazy val printResponse =
    (url: String) =>
      (response: CloseableHttpResponse) => {
        val out = new ByteArrayOutputStream()
        for {
          r <- Option(response)
          e <- Option(r.getEntity)
        } yield e.writeTo(out)
        println(
          s"""$url
             |========================================
             |${out.toString("utf-8")}
           """.stripMargin)
      }

  private lazy val withAuth =
    (method: AWSApiGatewayMethods) =>
      (authorize: AWSApiGatewayAuthorize) =>
        (authName: Option[String]) =>
          (resourceId: String) => Try {
            (authName map { name =>
              for {
                aOp <- authorize.getAuthorizer(name)
                r <- Try {
                  aOp map { a =>
                    method.updateMethod(
                      resourceId = resourceId,
                      "/authorizationType" -> "CUSTOM",
                      "/authorizerId" -> a.getId
                    ).get
                  } getOrElse(throw new RuntimeException(s"Custome Authorizer is nothing. $name"))
                }
              } yield r
            } getOrElse {
              method.updateMethod(
                resourceId = resourceId,
                "/authorizationType" -> "NONE"
              )
            }).get
            unit
          }

  private lazy val unit: Unit = {}

}
