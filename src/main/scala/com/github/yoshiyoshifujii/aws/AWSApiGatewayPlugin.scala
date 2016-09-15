package com.github.yoshiyoshifujii.aws

import com.amazonaws.services.apigateway.model.PutMode
import com.github.yoshiyoshifujii.aws.apigateway.AWSApiGatewayRestApi
import sbt._
import complete.DefaultParsers._
import Keys._

object AWSApiGatewayPlugin extends AutoPlugin {

  object autoImport {
    lazy val getRestApis = taskKey[Unit]("")
    lazy val createApiGateway = inputKey[Unit]("")
    lazy val deleteApiGateway = inputKey[Unit]("")
    lazy val putApiGateway = taskKey[Unit]("")
    lazy val deployStages = taskKey[Unit]("")
    lazy val createDeployment = inputKey[Unit]("")
    lazy val updateStages = inputKey[Unit]("")
    lazy val getStages = taskKey[Unit]("")
    lazy val getDeployments = taskKey[Unit]("")
    lazy val getResources = taskKey[Unit]("")

    lazy val awsRegion = settingKey[String]("")
    lazy val awsAccountId = settingKey[String]("")

    lazy val awsApiGatewayRestApiId = settingKey[String]("")
    lazy val awsApiGatewayYAMLFile = settingKey[File]("")
    lazy val awsApiGatewayStages = settingKey[Seq[(String, Option[String])]]("")
    lazy val awsApiGatewayStageVariables = settingKey[Map[String, Map[String, String]]]("")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    getRestApis := {
      val api = AWSApiGatewayRestApi(awsRegion.value)
      api.printGets.get
    },
    createApiGateway := {
      val api = AWSApiGatewayRestApi(awsRegion.value)
      val res = spaceDelimited("<arg>").parsed match {
        case Seq(restApiName) =>
          api.create(restApiName, None).get
        case Seq(restApiName, restApiDescription) =>
          api.create(restApiName, Some(restApiDescription)).get
        case _ =>
          sys.error(s"Error createApiGateway. useage: createApiGateway <name> [description]")
      }
      println(s"ApiGateway created: ${res.getId}")
    },
    deleteApiGateway := {
      val api = AWSApiGatewayRestApi(awsRegion.value)
      spaceDelimited("<arg>").parsed match {
        case Seq(restApiId) =>
          api.delete(restApiId).get
          println(s"ApiGateway deleted: $restApiId")
        case _ =>
          sys.error(s"Error deleteApiGateway. useage: deleteApiGateway <restApiId>")
      }
    },
    putApiGateway := {
      val api = AWSApiGatewayRestApi(awsRegion.value)
      api.put(
        restApiId = awsApiGatewayRestApiId.value,
        body = awsApiGatewayYAMLFile.value,
        mode = PutMode.Overwrite,
        failOnWarnings = None).get
    },
    deployStages := {
      val restApiId = awsApiGatewayRestApiId.value
      val variables = awsApiGatewayStageVariables.?.value
      val api = AWSApiGatewayRestApi(awsRegion.value)
      val stages = awsApiGatewayStages.value

      lazy val createDeployment = (sn: String, sd: Option[String]) =>
        api.createDeployment(
          restApiId = restApiId,
          stageName = sn,
          stageDescription = sd,
          description = Some(version.value),
          variables = variables.flatMap(m => m.get(sn))
        ).get.getId

      lazy val createOrUpdateStage = (deploymentId: String) => (sn: String, sd: Option[String]) =>
        api.createOrUpdateStage(
          restApiId = restApiId,
          stageName = sn,
          deploymentId = deploymentId,
          description = sd,
          variables = variables.flatMap(m => m.get(sn))
        ).get

      stages.headOption match {
        case Some((stageName, stageDescription)) =>
          val deploymentId = createDeployment(stageName, stageDescription)
          stages.tail.foreach { case (sn, sd) =>
            createOrUpdateStage(deploymentId)(sn, sd)
          }
        case None =>
      }
    },
    createDeployment := {
      val restApiId = awsApiGatewayRestApiId.value
      val variables = awsApiGatewayStageVariables.?.value
      val api = AWSApiGatewayRestApi(awsRegion.value)
      spaceDelimited("<arg>").parsed match {
        case Seq(stageName, stageDescription) =>
          api.createDeployment(
            restApiId = restApiId,
            stageName = stageName,
            stageDescription = Some(stageDescription),
            description = Some(version.value),
            variables = variables.flatMap(m => m.get(stageName))
          ).get
        case Seq(stageName) =>
          api.createDeployment(
            restApiId = restApiId,
            stageName = stageName,
            stageDescription = None,
            description = Some(version.value),
            variables = variables.flatMap(m => m.get(stageName))
          ).get
        case _ =>
          sys.error("Error createDeployment. useage: createDeployment <stageName> [stageDescription]")
      }
    },
    updateStages := {
      val restApiId = awsApiGatewayRestApiId.value
      val api = AWSApiGatewayRestApi(awsRegion.value)
      val stages = awsApiGatewayStages.value

      spaceDelimited("<arg>").parsed match {
        case Seq(deploymentId) =>
          stages.foreach {
            case (sn, sd) =>
              api.updateStage(
                restApiId = restApiId,
                stageName = sn,
                deploymentId = deploymentId).get
          }
        case _ =>
          sys.error("Error updateStages. useage: updateStages <deploymentId>")
      }
    },
    getStages := {
      AWSApiGatewayRestApi(awsRegion.value)
        .printStages(awsApiGatewayRestApiId.value)
        .get
    },
    getDeployments := {
      AWSApiGatewayRestApi(awsRegion.value)
        .printDeployments(awsApiGatewayRestApiId.value)
        .get
    },
    getResources := {
      AWSApiGatewayRestApi(awsRegion.value)
        .printResources(awsApiGatewayRestApiId.value)
        .get
    }
  )
}
