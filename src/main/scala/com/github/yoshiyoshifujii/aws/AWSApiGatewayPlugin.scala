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

    lazy val createDeployment = inputKey[Unit]("")

    lazy val deployStages = inputKey[Unit]("")
    lazy val getStages = taskKey[Unit]("")
    lazy val updateStage = inputKey[Unit]("")
    lazy val deleteStages = taskKey[Unit]("")
    lazy val deleteStage = inputKey[Unit]("")

    lazy val getDeployments = taskKey[Unit]("")
    lazy val deleteDeployments = taskKey[Unit]("")
    lazy val deleteDeployment = inputKey[Unit]("")

    lazy val getResources = taskKey[Unit]("")
    lazy val deleteResources = taskKey[Unit]("")
    lazy val deleteResource = inputKey[Unit]("")

    lazy val getAuthorizers = taskKey[Unit]("")

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
      val res = spaceDelimited("<name> [description]").parsed match {
        case Seq(restApiName) =>
          api.create(restApiName, None).get
        case Seq(restApiName, restApiDescription) =>
          api.create(restApiName, Some(restApiDescription)).get
        case _ =>
          sys.error(s"Error createApiGateway. useage: createApiGateway <name> [description]")
      }
      println(s"ApiGateway created: ${res.getId}")
    },
    deleteApiGateway := ? {
      val api = AWSApiGatewayRestApi(awsRegion.value)
      spaceDelimited("<restApiId>").parsed match {
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
        mode = PutMode.Merge,
        failOnWarnings = None).get
    },
    createDeployment := {
      val restApiId = awsApiGatewayRestApiId.value
      val variables = awsApiGatewayStageVariables.?.value
      val api = AWSApiGatewayRestApi(awsRegion.value)
      spaceDelimited("<stageName> [stageDescription]").parsed match {
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
    deployStages := {
      val restApiId = awsApiGatewayRestApiId.value
      val variables = awsApiGatewayStageVariables.?.value
      val api = AWSApiGatewayRestApi(awsRegion.value)
      val stages = awsApiGatewayStages.value

      lazy val createOrUpdateStage = (deploymentId: String) => (sn: String, sd: Option[String]) =>
        api.createOrUpdateStage(
          restApiId = restApiId,
          stageName = sn,
          deploymentId = deploymentId,
          description = sd,
          variables = variables.flatMap(m => m.get(sn))
        ).get

      spaceDelimited("<deploymentId>").parsed match {
        case Seq(deploymentId) =>
          stages.foreach {
            case (sn, sd) =>
              createOrUpdateStage(deploymentId)(sn, sd)
          }
        case _ =>
          sys.error("Error deployStages. useage: deployStages <deploymentId>")
      }
    },
    getStages := {
      AWSApiGatewayRestApi(awsRegion.value)
        .printStages(awsApiGatewayRestApiId.value)
        .get
    },
    updateStage := {
      spaceDelimited("<stageName> <deploymentId>").parsed match {
        case Seq(stageName, deploymentId) =>
          AWSApiGatewayRestApi(awsRegion.value)
            .updateStage(awsApiGatewayRestApiId.value, stageName, deploymentId)
            .get
        case _ =>
          sys.error("Error updateStage. useage: updateStage <stageName> <deploymentId>")
      }
    },
    deleteStages := ? {
      AWSApiGatewayRestApi(awsRegion.value)
        .deleteStages(awsApiGatewayRestApiId.value)
        .get
    },
    deleteStage := ? {
      spaceDelimited("<stageName>").parsed match {
        case Seq(stageName) =>
          AWSApiGatewayRestApi(awsRegion.value)
            .deleteStage(awsApiGatewayRestApiId.value, stageName)
            .get
        case _ =>
          sys.error("Error deleteStage. useage: deleteStage <stageName>")
      }
    },
    getDeployments := {
      AWSApiGatewayRestApi(awsRegion.value)
        .printDeployments(awsApiGatewayRestApiId.value)
        .get
    },
    deleteDeployments := ? {
      AWSApiGatewayRestApi(awsRegion.value)
        .deleteDeployments(awsApiGatewayRestApiId.value)
        .get
    },
    deleteDeployment := ? {
      spaceDelimited("<deploymentId>").parsed match {
        case Seq(deploymentId) =>
          AWSApiGatewayRestApi(awsRegion.value)
            .deleteDeployment(awsApiGatewayRestApiId.value, deploymentId)
            .get
        case _ =>
          sys.error("Error deleteDeployment. useage: deleteDeployment <deploymentId>")
      }
    },
    getResources := {
      AWSApiGatewayRestApi(awsRegion.value)
        .printResources(awsApiGatewayRestApiId.value)
        .get
    },
    deleteResources := ? {
      AWSApiGatewayRestApi(awsRegion.value)
        .deleteResources(awsApiGatewayRestApiId.value)
        .get
    },
    deleteResource := ? {
      spaceDelimited("<resourceId>").parsed match {
        case Seq(resourceId) =>
          AWSApiGatewayRestApi(awsRegion.value)
            .deleteResource(awsApiGatewayRestApiId.value, resourceId)
            .get
        case _ =>
          sys.error("Error deleteResource. useage: deleteResource <resourceId>")
      }
    },
    getAuthorizers := {
      AWSApiGatewayRestApi(awsRegion.value)
        .printAuthorizers(awsApiGatewayRestApiId.value)
        .get
    }
  )
}
