package com.github.yoshiyoshifujii.aws.apigateway

import java.io.File

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.apigateway.AmazonApiGatewayClient
import com.amazonaws.services.apigateway.model._
import com.github.yoshiyoshifujii.aws.AWSWrapper
import com.github.yoshiyoshifujii.cliformatter.CliFormatter

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.Try

trait AWSApiGatewayWrapper extends AWSWrapper {

  type Region = String
  type RestApiId = String
  type StageName = String
  type StageDescription = String
  type DeploymentId = String
  type StageVariables = Map[String, String]

  val regionName: String
  lazy val client = {
    val c = new AmazonApiGatewayClient()
    c.setRegion(RegionUtils.getRegion(regionName))
    c
  }

  protected def toOpt[A](f: => A) =
    try {
      Some(f)
    } catch {
      case e: NotFoundException => None
    }
}

trait AWSApiGatewayRestApiWrapper extends AWSApiGatewayWrapper {

  def create(name: String,
             description: Option[String]) = Try {
    val request = new CreateRestApiRequest()
      .withName(name)
    description.foreach(request.setDescription)

    client.createRestApi(request)
  }

  def delete(restApiId: RestApiId) = Try {
    val request = new DeleteRestApiRequest()
      .withRestApiId(restApiId)

    client.deleteRestApi(request)
  }

  def get(restApiId: RestApiId) = Try {
    val request = new GetRestApiRequest()
      .withRestApiId(restApiId)

    toOpt(client.getRestApi(request))
  }

  def gets = Try {
    val request = new GetRestApisRequest()

    client.getRestApis(request)
  }

  def `import`(body: File,
               failOnWarnings: Option[Boolean]) = Try {
    val request = new ImportRestApiRequest()
      .withBody(toByteBuffer(body))
    failOnWarnings.foreach(request.setFailOnWarnings(_))

    client.importRestApi(request)
  }

  def put(restApiId: RestApiId,
          body: File,
          mode: PutMode,
          failOnWarnings: Option[Boolean]) = Try {
    val request = new PutRestApiRequest()
      .withRestApiId(restApiId)
      .withBody(toByteBuffer(body))
      .withMode(mode)
    failOnWarnings.foreach(request.setFailOnWarnings(_))

    client.putRestApi(request)
  }

  def createDeployment(restApiId: RestApiId,
                       stageName: StageName,
                       stageDescription: Option[StageDescription],
                       description: Option[String],
                       variables: Option[StageVariables]) = Try {
    val request = new CreateDeploymentRequest()
      .withRestApiId(restApiId)
      .withStageName(stageName)
    stageDescription.foreach(request.setStageDescription)
    description.foreach(request.setDescription)
    variables.foreach(v => request.setVariables(v.asJava))

    client.createDeployment(request)
  }

  def createStage(restApiId: RestApiId,
                  stageName: StageName,
                  deploymentId: DeploymentId,
                  description: Option[StageDescription],
                  variables: Option[StageVariables]) = Try {
    val request = new CreateStageRequest()
      .withRestApiId(restApiId)
      .withStageName(stageName)
      .withDeploymentId(deploymentId)
    description.foreach(request.setDescription)
    variables.foreach(v => request.setVariables(v.asJava))

    client.createStage(request)
  }

  def getStage(restApiId: RestApiId,
               stageName: StageName) = Try {
    val request = new GetStageRequest()
      .withRestApiId(restApiId)
      .withStageName(stageName)

    toOpt(client.getStage(request))
  }

  def updateStage(restApiId: RestApiId,
                  stageName: StageName,
                  deploymentId: DeploymentId) = Try {
    val po = new PatchOperation()
      .withOp(Op.Replace)
      .withPath("/deploymentId")
      .withValue(deploymentId)

    val request = new UpdateStageRequest()
      .withRestApiId(restApiId)
      .withStageName(stageName)
      .withPatchOperations(po)

    client.updateStage(request)
  }

  def createOrUpdateStage(restApiId: RestApiId,
                          stageName: StageName,
                          deploymentId: DeploymentId,
                          description: Option[StageDescription],
                          variables: Option[StageVariables]) = {
    for {
      sOp <- getStage(restApiId, stageName)
      res <- Try {
        sOp map { s =>
          updateStage(
            restApiId = restApiId,
            stageName = stageName,
            deploymentId = deploymentId
          ).get.getDeploymentId
        } getOrElse {
          createStage(
            restApiId = restApiId,
            stageName = stageName,
            deploymentId = deploymentId,
            description = description,
            variables = variables).get.getDeploymentId
        }
      }
    } yield res
  }

  def getDeployments(restApiId: RestApiId) = Try {
    val request = new GetDeploymentsRequest()
      .withRestApiId(restApiId)

    client.getDeployments(request)
  }

  def printDeployments(restApiId: RestApiId) = {
    for {
      l <- getDeployments(restApiId)
    } yield {
      val p = CliFormatter(
        restApiId,
        "Created Date" -> 30,
        "Deployment Id" -> 15,
        "Description" -> 30
      ).print3(
        l.getItems map { d =>
          (d.getCreatedDate.toString, d.getId, d.getDescription)
        }: _*)
      println(p)
    }
  }

  def getStages(restApiId: RestApiId) = Try {
    val request = new GetStagesRequest()
      .withRestApiId(restApiId)

    client.getStages(request)
  }

  def printStages(restApiId: RestApiId) = {
    for {
      l <- getStages(restApiId)
    } yield {
      val p = CliFormatter(
        restApiId,
        "Stage Name" -> 10,
        "Last Updated Date" -> 30,
        "Deployment Id" -> 15,
        "Description" -> 30
      ).print4(
        l.getItem map { s =>
          (s.getStageName, s.getLastUpdatedDate.toString, s.getDeploymentId, s.getDescription)
        }: _*)
      println(p)
    }
  }
}
class AWSApiGatewayRestApi(val regionName: String) extends AWSApiGatewayRestApiWrapper

case class Uri(regionName: String,
               awsAccountId: String,
               lambdaName: String,
               lambdaAlias: Option[String]) {
  def value = Seq(
    "arn",
    "aws",
    "apigateway",
    regionName,
    "lambda",
    "path/2015-03-31/functions/arn",
    "aws",
    "lambda",
    regionName,
    awsAccountId,
    "function",
    lambdaAlias
      .map(a => s"$lambdaName:$a/invocations")
      .getOrElse(s"$lambdaName/invocations")
  ).mkString(":")
}

case class RequestTemplates(values: (String, String)*) {
  def toMap = values.toMap
}

case class ResponseTemplates(values: ResponseTemplate*)

case class ResponseTemplate(statusCode: String,
                            selectionPattern: Option[String],
                            templates: (String, String)*)

trait AWSApiGatewayMethodsWrapper extends AWSApiGatewayWrapper {

  type ResourceId = String
  type HttpMethod = String
  type StatusCode = String
  type SelectionPattern = String

  def putIntegration(restApiId: RestApiId,
                     resourceId: ResourceId,
                     httpMethod: HttpMethod,
                     uri: Uri,
                     requestTemplates: RequestTemplates) = Try {
    val request = new PutIntegrationRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withType(IntegrationType.AWS)
      .withIntegrationHttpMethod("POST")
      .withUri(uri.value)
      .withRequestTemplates(requestTemplates.toMap)
      .withPassthroughBehavior("WHEN_NO_TEMPLATES")

    client.putIntegration(request)
  }

  def putIntegrationResponse(restApiId: RestApiId,
                             resourceId: ResourceId,
                             httpMethod: HttpMethod,
                             statusCode: StatusCode,
                             selectionPattern: Option[SelectionPattern],
                             responseTemplates: (String, String)*) = Try {
    val request = new PutIntegrationResponseRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withStatusCode(statusCode)
    selectionPattern.foreach(request.setSelectionPattern)

    if (responseTemplates.nonEmpty)
      request.setResponseTemplates(responseTemplates.toMap.asJava)

    client.putIntegrationResponse(request)
  }

  def getIntegration(restApiId: RestApiId,
                     resourceId: ResourceId,
                     httpMethod: HttpMethod) = Try {
    val request = new GetIntegrationRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)

    toOpt(client.getIntegration(request))
  }

  def getResources(restApiId: RestApiId) = Try {
    val request = new GetResourcesRequest()
      .withRestApiId(restApiId)

    client.getResources(request)
  }

  def getResource(restApiId: RestApiId,
                  path: String) = {
    for {
      resources <- getResources(restApiId)
    } yield resources.getItems.find(_.getPath == path)
  }

  def deploy(restApiId: RestApiId,
             path: String,
             httpMethod: String,
             uri: Uri,
             requestTemplates: RequestTemplates,
             responseTemplates: ResponseTemplates): Try[Option[Resource]] = {
    for {
      resource <- getResource(restApiId, path)
      r <- Try {
        for {
          r <- resource
          resourceId = r.getId
        } yield (for {
          i <- putIntegration(restApiId, resourceId, httpMethod, uri, requestTemplates)
          irs <- Try {
            responseTemplates.values map { resT =>
              putIntegrationResponse(restApiId, resourceId, httpMethod, resT.statusCode, resT.selectionPattern, resT.templates: _*).get
            }
          }
        } yield r).get
      }
    } yield r
  }
}
class AWSApiGatewayMethods(val regionName: String) extends AWSApiGatewayMethodsWrapper

