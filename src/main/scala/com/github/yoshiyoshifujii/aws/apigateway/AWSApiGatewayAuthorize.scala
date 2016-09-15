package com.github.yoshiyoshifujii.aws.apigateway

import com.amazonaws.services.apigateway.model._
import com.github.yoshiyoshifujii.cliformatter.CliFormatter

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.Try

trait AWSApiGatewayAuthorizeWrapper extends AWSApiGatewayWrapper {
  val restApiId: RestApiId

  def createAuthorizer(name: String,
                       authorizerUri: Uri,
                       identitySourceHeaderName: String,
                       identityValidationExpression: Option[String],
                       authorizerResultTtlInSeconds: Option[Int] = Some(300)) = Try {
    val request = new CreateAuthorizerRequest()
      .withRestApiId(restApiId)
      .withName(name)
      .withType(AuthorizerType.TOKEN)
      .withAuthType("custom")
      .withAuthorizerUri(authorizerUri.value)
      .withIdentitySource(IdentitySource(identitySourceHeaderName).mkValue)
    identityValidationExpression.foreach(request.setIdentityValidationExpression)
    authorizerResultTtlInSeconds.foreach(request.setAuthorizerResultTtlInSeconds(_))

    client.createAuthorizer(request)
  }

  def getAuthorizers = Try {
    val request = new GetAuthorizersRequest()
      .withRestApiId(restApiId)

    client.getAuthorizers(request)
  }

  def printAuthorizers =
    for {
      l <- getAuthorizers
    } yield {
      val p = CliFormatter(
        s"Rest API Authorizers: $restApiId",
        "ID" -> 15,
        "Name" -> 40,
        "URI" -> 150
      ).print3(
        l.getItems map { d =>
          (d.getId, d.getName, d.getAuthorizerUri)
        }: _*)
      println(p)
    }

  def getAuthorizer(name: String) =
    for {
      as <- getAuthorizers
    } yield as.getItems.find(a => a.getName == name)

  def updateAuthorizer(authorizerId: AuthorizerId,
                       name: String,
                       authorizerUri: Uri,
                       identitySourceHeaderName: String,
                       identityValidationExpression: Option[String],
                       authorizerResultTtlInSeconds: Option[Int] = Some(300)) = Try {
    lazy val generatePatch =
      (p: String) =>
        (v: String) => Option {
          new PatchOperation()
            .withOp(Op.Replace)
            .withPath(p)
            .withValue(v)
        }

    lazy val patchOperations = Seq(
      generatePatch("/name")(name),
      generatePatch("/authorizerUri")(authorizerUri.value),
      generatePatch("/identitySource")(IdentitySource(identitySourceHeaderName).mkValue),
      identityValidationExpression.flatMap(generatePatch("/identityValidationExpression")(_)),
      authorizerResultTtlInSeconds.flatMap(i => generatePatch("/authorizerResultTtlInSeconds")(i.toString))
    ).flatten

    val request = new UpdateAuthorizerRequest()
      .withRestApiId(restApiId)
      .withAuthorizerId(authorizerId)
      .withPatchOperations(patchOperations.asJava)

    client.updateAuthorizer(request)
  }

  def deployAuthorizer(name: String,
                       authorizerUri: Uri,
                       identitySourceHeaderName: String,
                       identityValidationExpression: Option[String],
                       authorizerResultTtlInSeconds: Option[Int] = Some(300)) = {
    for {
      aOp <- getAuthorizer(name)
      id <- Try {
        aOp map { a =>
          updateAuthorizer(
            authorizerId = a.getId,
            name = name,
            authorizerUri = authorizerUri,
            identitySourceHeaderName = identitySourceHeaderName,
            identityValidationExpression = identityValidationExpression,
            authorizerResultTtlInSeconds = authorizerResultTtlInSeconds
          ).get.getId
        } getOrElse {
          createAuthorizer(
            name = name,
            authorizerUri = authorizerUri,
            identitySourceHeaderName = identitySourceHeaderName,
            identityValidationExpression = identityValidationExpression,
            authorizerResultTtlInSeconds = authorizerResultTtlInSeconds
          ).get.getId
        }
      }
    } yield id
  }

}
case class AWSApiGatewayAuthorize(regionName: String,
                                  restApiId: RestApiId) extends AWSApiGatewayAuthorizeWrapper

