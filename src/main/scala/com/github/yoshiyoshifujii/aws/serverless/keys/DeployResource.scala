package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.IntegrationType
import com.github.yoshiyoshifujii.aws.apigateway.{
  AWSApiGatewayAuthorize,
  AWSApiGatewayMethods,
  RequestTemplates,
  RestApiId
}
import serverless.{FunctionBase, HttpEvent}

trait DeployResource extends KeysBase {

  protected def deployResource(restApiId: RestApiId,
                               function: FunctionBase,
                               lambdaSuffix: String,
                               publishedVersion: Option[String],
                               httpEvent: HttpEvent) = {
    val method = AWSApiGatewayMethods(
      regionName = so.provider.region,
      restApiId = restApiId,
      integrationType =
        if (httpEvent.proxyIntegration) IntegrationType.AWS_PROXY else IntegrationType.AWS,
      path = httpEvent.path,
      httpMethod = httpEvent.method
    )

    for {
      resourceOpt <- method.deploy(
        awsAccountId = so.provider.awsAccount,
        lambdaName = function.nameWith(lambdaSuffix),
        lambdaAlias = publishedVersion,
        requestTemplates = RequestTemplates(httpEvent.request.templateToSeq: _*),
        responseTemplates = httpEvent.response.templates,
        withAuth = withAuth(method)(AWSApiGatewayAuthorize(so.provider.region, restApiId))(
          httpEvent.authorizerName),
        cors = httpEvent.cors
      )
      _ = { resourceOpt.foreach(r => println(s"Resource: ${r.toString}")) }
    } yield ()
  }

}
