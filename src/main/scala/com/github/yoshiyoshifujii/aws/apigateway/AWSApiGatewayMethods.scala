package com.github.yoshiyoshifujii.aws.apigateway

import com.amazonaws.services.apigateway.model._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.Try

trait AWSApiGatewayMethodsWrapper extends AWSApiGatewayRestApiWrapper {

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
case class AWSApiGatewayMethods(regionName: String) extends AWSApiGatewayMethodsWrapper

