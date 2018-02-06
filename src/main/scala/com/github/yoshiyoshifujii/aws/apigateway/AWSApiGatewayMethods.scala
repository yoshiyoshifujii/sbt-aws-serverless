package com.github.yoshiyoshifujii.aws.apigateway

import com.amazonaws.services.apigateway.model._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.Try

trait AWSApiGatewayMethodsWrapper extends AWSApiGatewayRestApiWrapper {
  val restApiId: RestApiId
  val integrationType: IntegrationType
  val path: Path
  val httpMethod: HttpMethod

  def putIntegration(resourceId: ResourceId, uri: Uri, requestTemplates: RequestTemplates) = Try {
    val request = new PutIntegrationRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withType(integrationType)
      .withIntegrationHttpMethod("POST")
      .withUri(uri.value)
      .withRequestTemplates(requestTemplates.toMap)
      .withPassthroughBehavior("WHEN_NO_TEMPLATES")

    client.putIntegration(request)
  }

  def putIntegrationMock(resourceId: ResourceId) = Try {
    val request = new PutIntegrationRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withType(IntegrationType.MOCK)
      .withRequestTemplates(Map("application/json" -> """{"statusCode": 200}""").asJava)
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
                             responseParameters: Map[String, String],
                             responseTemplates: Map[String, String]) = Try {
    val request = new PutIntegrationResponseRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withStatusCode(statusCode)
    selectionPattern.foreach(request.setSelectionPattern)

    if (responseParameters.nonEmpty)
      request.setResponseParameters(
        responseParameters.map(m => s"method.response.header.${m._1}" -> m._2).asJava)

    if (responseTemplates.nonEmpty)
      request.setResponseTemplates(responseTemplates.asJava)

    client.putIntegrationResponse(request)
  }

  def deleteIntegrationResponse(resourceId: ResourceId) = Try {
    val request = new DeleteIntegrationResponseRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)

    client.deleteIntegrationResponse(request)
  }

  def putIntegrationResponses(resourceId: ResourceId, responseTemplates: ResponseTemplates) = Try {

    responseTemplates.values map { resT =>
      putIntegrationResponse(
        resourceId,
        resT.statusCode,
        resT.selectionPattern,
        resT.parameters,
        resT.templates
      ).get
    }
  }

  def putMethodResponse(resourceId: ResourceId,
                        statusCode: StatusCode,
                        responseParameters: (String, Boolean)*) = Try {
    val request = new PutMethodResponseRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withStatusCode(statusCode)

    responseParameters.foreach {
      case (k, v) =>
        request.addResponseParametersEntry(k, v)
    }

    client.putMethodResponse(request)
  }

  def getMethodResponse(resourceId: ResourceId, statusCode: StatusCode) = Try {
    val request = new GetMethodResponseRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withStatusCode(statusCode)

    client.getMethodResponse(request)
  }

  def updateMethodResponse(resourceId: ResourceId,
                           statusCode: StatusCode,
                           patchOperatios: (PatchPath, PatchValue, Op)*) = Try {
    val request = new UpdateMethodResponseRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withStatusCode(statusCode)
      .withPatchOperations(patchOperatios.map(g => generatePatch(g._1)(g._2)(g._3)).asJava)

    client.updateMethodResponse(request)
  }

  def getResource =
    for {
      resources <- getResources(restApiId)
    } yield resources.find(_.getPath == path)

  def getMethod(resourceId: ResourceId) = Try {
    val request = new GetMethodRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)

    toOpt(client.getMethod(request))
  }

  def putMethod(resourceId: ResourceId, getMethodResult: GetMethodResult) = Try {
    val request = new PutMethodRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withAuthorizationType("NONE")
      .withRequestParameters(getMethodResult.getRequestParameters)

    client.putMethod(request)
  }

  def updateMethod(resourceId: ResourceId, patchOperatios: (PatchPath, PatchValue)*) = Try {

    val request = new UpdateMethodRequest()
      .withRestApiId(restApiId)
      .withResourceId(resourceId)
      .withHttpMethod(httpMethod)
      .withPatchOperations(patchOperatios.map(g => generatePatch(g._1)(g._2)(Op.Replace)).asJava)

    client.updateMethod(request)
  }

  def enableCORS(resourceId: ResourceId) = {
    val om = AWSApiGatewayMethods(regionName = this.regionName,
                                  restApiId = this.restApiId,
                                  integrationType = this.integrationType,
                                  path = this.path,
                                  httpMethod = "OPTIONS")
    for {
      methodOpt  <- getMethod(resourceId)
      optionsOpt <- om.getMethod(resourceId)
      _ <- Try {
        methodOpt foreach { method =>
          // add header
          method.getMethodResponses.asScala.values.map { mr =>
            updateMethodResponse(
              resourceId = resourceId,
              statusCode = mr.getStatusCode,
              ("/responseParameters/method.response.header.Access-Control-Allow-Origin",
               "true",
               Op.Add)
            ).get
          }

          // add options
          if (optionsOpt.isEmpty) {
            (for {
              _ <- om.putMethod(resourceId, method)
              _ <- om.putMethodResponse(
                resourceId,
                "200",
                "method.response.header.Access-Control-Allow-Methods"     -> false,
                "method.response.header.Access-Control-Allow-Credentials" -> false,
                "method.response.header.Access-Control-Allow-Origin"      -> false,
                "method.response.header.Access-Control-Allow-Headers"     -> false
              )
            } yield ()).get
          }

          (for {
            _ <- om.putIntegrationMock(resourceId)
            _ <- om.putIntegrationResponse(
              resourceId = resourceId,
              statusCode = "200",
              selectionPattern = None,
              responseParameters = Map(
                "Access-Control-Allow-Methods"     -> "'DELETE,GET,HEAD,PATCH,POST,PUT,OPTIONS'",
                "Access-Control-Allow-Credentials" -> "'true'",
                "Access-Control-Allow-Origin"      -> "'*'",
                "Access-Control-Allow-Headers"     -> "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"
              ),
              responseTemplates = Map.empty
            )
          } yield ()).get
        }
      }
    } yield ()
  }

  def deploy(awsAccountId: String,
             lambdaName: String,
             lambdaAlias: Option[String],
             requestTemplates: RequestTemplates,
             responseTemplates: ResponseTemplates,
             withAuth: ResourceId => Try[Unit] = resourceId => Try(),
             cors: Boolean = false): Try[Option[Resource]] = {
    val uri = Uri(regionName, awsAccountId, lambdaName, lambdaAlias)
    for {
      resource <- getResource
      resourceOpt <- Try {
        resource map { r =>
          val resourceId = r.getId
          (for {
            _ <- putIntegration(resourceId, uri, requestTemplates)
            _ <- Try {
              if (cors) {
                enableCORS(resourceId).get
              } else {
                Seq.empty
              }
            }
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

  private lazy val generatePatch =
    (path: PatchPath) =>
      (value: PatchValue) =>
        (op: Op) =>
          new PatchOperation()
            .withOp(op)
            .withPath(path)
            .withValue(value)

}

case class AWSApiGatewayMethods(regionName: String,
                                restApiId: RestApiId,
                                integrationType: IntegrationType,
                                path: Path,
                                httpMethod: HttpMethod)
    extends AWSApiGatewayMethodsWrapper
