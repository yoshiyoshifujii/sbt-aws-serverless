package com.github.yoshiyoshifujii.aws.serverless.keys

import com.github.yoshiyoshifujii.aws.apigateway.{AWSApiGatewayAuthorize, RestApiId}
import serverless.{AuthorizeEvent, FunctionBase}

trait DeployAuthorizer extends KeysBase {

  protected def deployAuthorizer(restApiId: RestApiId,
                                 function: FunctionBase,
                                 lambdaSuffix: String,
                                 authorizeEvent: AuthorizeEvent) = {
    lazy val authorize = AWSApiGatewayAuthorize(
      so.provider.region,
      restApiId
    )
    for {
      authId <- authorize.deployAuthorizer(
        name = authorizeEvent.name,
        awsAccountId = so.provider.awsAccount,
        lambdaName = function.nameWith(lambdaSuffix),
        lambdaAlias = None,
        identitySourceHeaderName = authorizeEvent.identitySourceHeaderName,
        identityValidationExpression = authorizeEvent.identityValidationExpression,
        authorizerResultTtlInSeconds = Option(authorizeEvent.resultTtlInSeconds)
      )
      _ = { println(s"Authorizer: $authId") }
    } yield ()
  }

}
