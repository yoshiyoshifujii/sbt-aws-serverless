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

  def getIntegration(resourceId: ResourceId) = Try {
    val request = new GetIntegrationRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)

    toOpt(client.getIntegration(request))
  }

  def getResource =
    for {
      resources <- getResources(restApiId)
    } yield resources.getItems.find(_.getPath == path)

  def deploy(uri: Uri,
             requestTemplates: RequestTemplates,
             responseTemplates: ResponseTemplates): Try[Option[Resource]] = {
    for {
      resource <- getResource
      r <- Try {
        resource map { r =>
          val resourceId = r.getId
          (for {
            i <- putIntegration(resourceId, uri, requestTemplates)
            irs <- putIntegrationResponses(resourceId, responseTemplates)
          } yield r).get
        }
      }
    } yield r
  }
}
case class AWSApiGatewayMethods(regionName: String,
                                restApiId: RestApiId,
                                path: Path,
                                httpMethod: HttpMethod) extends AWSApiGatewayMethodsWrapper

