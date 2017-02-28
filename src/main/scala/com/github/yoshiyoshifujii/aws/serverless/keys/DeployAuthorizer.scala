package com.github.yoshiyoshifujii.aws.serverless.keys

import com.github.yoshiyoshifujii.aws.apigateway.{AWSApiGatewayAuthorize, RestApiId}
import serverless.{AuthorizeEvent, FunctionBase}

trait DeployAuthorizer extends KeysBase {

  protected def deployAuthorizer(restApiId: RestApiId,
                                 function: FunctionBase,
                                 lambdaAlias: Option[String],
                                 authorizeEvent: AuthorizeEvent) = {
    lazy val authorize = AWSApiGatewayAuthorize(
      so.provider.region, restApiId
    )
    for {
      authId <- authorize.deployAuthorizer(
        name = authorizeEvent.name,
        awsAccountId = so.provider.awsAccount,
        lambdaName = function.name,
        lambdaAlias = lambdaAlias,
        identitySourceHeaderName = authorizeEvent.identitySourceHeaderName,
        identityValidationExpression = authorizeEvent.identityValidationExpression,
        authorizerResultTtlInSeconds = Option(authorizeEvent.resultTtlInSeconds)
      )
      _ = { println(s"Authorizer: $authId") }
    } yield ()
  }

}
