package serverless

abstract class RequestTemplate(contentType: String,
                               template: String) {
  def toTuple: (String, String) = contentType -> template
}

object RequestTemplate {

  case object ApplicationJson extends RequestTemplate(
    "application/json", DefaultRequestTemplate.AllParameters)

  case object ApplicationXWwwFormUrlencoded extends RequestTemplate(
    "application/x-www-form-urlencoded", DefaultRequestTemplate.AllParameters)

}

case class Request(templates: RequestTemplate*) {

  def templateToSeq: Seq[(String, String)] =
    templates.map(_.toTuple)

}

object Request {
  def apply(): Request = new Request(RequestTemplate.ApplicationJson)
}

object DefaultRequestTemplate {

  val AllParameters =
    """
      |##  See http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-mapping-template-reference.html
      |##  This template will pass through all parameters including path, querystring, header, stage variables, and context through to the integration endpoint via the body/payload
      |#set(\$allParams = \$input.params())
      |{
      |"body-json" : \$input.json('\$'),
      |"params" : {
      |#foreach(\$type in \$allParams.keySet())
      |    #set(\$params = \$allParams.get(\$type))
      |"\$type" : {
      |    #foreach(\$paramName in \$params.keySet())
      |    "\$paramName" : "\$util.escapeJavaScript(\$params.get(\$paramName))"
      |        #if(\$foreach.hasNext),#end
      |    #end
      |}
      |    #if(\$foreach.hasNext),#end
      |#end
      |},
      |"stage-variables" : {
      |#foreach(\$key in \$stageVariables.keySet())
      |"\$key" : "\$util.escapeJavaScript(\$stageVariables.get(\$key))"
      |    #if(\$foreach.hasNext),#end
      |#end
      |},
      |"context" : {
      |    "account-id" : "\$context.identity.accountId",
      |    "api-id" : "\$context.apiId",
      |    "api-key" : "\$context.identity.apiKey",
      |    "caller" : "\$context.identity.caller",
      |    "cognito-authentication-provider" : "\$context.identity.cognitoAuthenticationProvider",
      |    "cognito-authentication-type" : "\$context.identity.cognitoAuthenticationType",
      |    "cognito-identity-id" : "\$context.identity.cognitoIdentityId",
      |    "cognito-identity-pool-id" : "\$context.identity.cognitoIdentityPoolId",
      |    "http-method" : "\$context.httpMethod",
      |    "stage" : "\$context.stage",
      |    "source-ip" : "\$context.identity.sourceIp",
      |    "user" : "\$context.identity.user",
      |    "user-agent" : "\$context.identity.userAgent",
      |    "user-arn" : "\$context.identity.userArn",
      |    "request-id" : "\$context.requestId",
      |    "resource-id" : "\$context.resourceId",
      |    "resource-path" : "\$context.resourcePath"
      |    },
      |#set (\$principalObj = \$util.parseJson(\$context.authorizer.principalId))
      |"authorizer" : {
      |#foreach(\$pkey in \$principalObj.keySet())
      |"\$pkey" : "\$util.escapeJavaScript(\$principalObj.get(\$pkey))"
      |    #if(\$foreach.hasNext),#end
      |#end
      |    }
      |}
    """.stripMargin

}