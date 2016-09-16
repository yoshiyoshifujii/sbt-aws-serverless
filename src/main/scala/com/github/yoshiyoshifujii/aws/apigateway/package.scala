package com.github.yoshiyoshifujii.aws

package object apigateway {

  type Region = String
  type RestApiId = String
  type StageName = String
  type StageDescription = String
  type DeploymentId = String
  type StageVariables = Map[String, String]
  type ResourceId = String
  type Path = String
  type HttpMethod = String
  type StatusCode = String
  type SelectionPattern = String
  type AuthorizerId = String
  type PatchPath = String
  type PatchValue = String

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

  case class IdentitySource(header: String) {
    lazy val mkValue = s"method.request.header.$header"
  }

}
