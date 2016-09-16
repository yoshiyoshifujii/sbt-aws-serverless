package com.github.yoshiyoshifujii.aws.apigateway

import com.amazonaws.services.apigateway.model._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.Try

trait AWSApiGatewayMethodsWrapper extends AWSApiGatewayRestApiWrapper {
  val restApiId: RestApiId
  val path: Path
  val httpMethod: HttpMethod

  def putIntegration(resourceId: ResourceId,
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

  def getIntegration(resourceId: ResourceId) = Try {
    val request = new GetIntegrationRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)

    toOpt(client.getIntegration(request))
  }

  def deleteIntegration(resourceId: ResourceId) = Try {
    val request = new DeleteIntegrationRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)

    client.deleteIntegration(request)
  }

  def putIntegrationResponse(resourceId: ResourceId,
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

  def deleteIntegrationResponse(resourceId: ResourceId) = Try {
    val request = new DeleteIntegrationResponseRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)

    client.deleteIntegrationResponse(request)
  }

  def putIntegrationResponses(resourceId: ResourceId,
                              responseTemplates: ResponseTemplates) = Try {
    responseTemplates.values map { resT =>
      putIntegrationResponse(
        resourceId,
        resT.statusCode,
        resT.selectionPattern,
        resT.templates: _*
      ).get
    }
  }

  def getResource =
    for {
      resources <- getResources(restApiId)
    } yield resources.getItems.find(_.getPath == path)

  def updateMethod(resourceId: ResourceId,
                   patchOperatios: (PatchPath, PatchValue)*) = Try {

    lazy val generatePatch =
      (path: PatchPath) =>
        (value: PatchValue) =>
          new PatchOperation()
            .withOp(Op.Replace)
            .withPath(path)
            .withValue(value)

    val request = new UpdateMethodRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withPatchOperations(patchOperatios.map(g => generatePatch(g._1)(g._2)).asJava)

    client.updateMethod(request)
  }

  def deploy(uri: Uri,
             requestTemplates: RequestTemplates,
             responseTemplates: ResponseTemplates,
             withAuth: ResourceId => Try[Unit] = resourceId => Try()): Try[Option[Resource]] = {
    for {
      resource <- getResource
      resourceOpt <- Try {
        resource map { r =>
          val resourceId = r.getId
          (for {
            _ <- putIntegration(resourceId, uri, requestTemplates)
            _ <- putIntegrationResponses(resourceId, responseTemplates)
            _ <- withAuth(resourceId)
          } yield r).get
        }
      }
    } yield resourceOpt
  }

  def upDeploy(withAuth: ResourceId => Try[Unit] = resourceId => Try()) = {
    for {
      resource <- getResource
      resourceOpt <- Try {
        resource map { r =>
          val resourceId = r.getId
          (for {
            _ <- deleteIntegrationResponse(resourceId)
            _ <- deleteIntegration(resourceId)
            _ <- withAuth(resourceId)
          } yield r).get
        }
      }
    } yield resourceOpt
  }

}
case class AWSApiGatewayMethods(regionName: String,
                                restApiId: RestApiId,
                                path: Path,
                                httpMethod: HttpMethod) extends AWSApiGatewayMethodsWrapper

