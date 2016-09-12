import com.github.yoshiyoshifujii.aws.apigateway._

val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8",
  organization := "com.github.yoshiyoshifujii.sample"
)

val assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case "application.conf" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
  publishArtifact in (Compile, packageBin) := false,
  publishArtifact in (Compile, packageSrc) := false,
  publishArtifact in (Compile, packageDoc) := false
)

val awsSettings = Seq(
  awsRegion := "us-east-1",
  awsAccountId := ""
)

val apiGatewaySettings = Seq(
  awsApiGatewayRestApiId := "",
  awsApiGatewayYAMLFile := file("./swagger.yaml"),
  awsApiGatewayResourceUriLambdaAlias := "${stageVariables.env}",
  awsApiGatewayStages := Seq(
    "it" -> Some("integration stage"),
    "ops" -> None,
    "staging" -> Some("staging stage")
  ),
  awsApiGatewayStageVariables := Map(
    "it" -> Map("env" -> "it", "region" -> "us-east-1"),
    "ops" -> Map("env" -> "ops", "region" -> "us-east-1"),
    "staging" -> Map("env" -> "staging", "region" -> "us-east-1")
  )
)

val lambdaSettings = Seq(
  awsLambdaFunctionName := s"${name.value}",
  awsLambdaRole := "",
  awsLambdaS3Bucket := "us-lambda-modules.huzi.me",
  awsLambdaDeployDescription := s"${version.value}",
  awsLambdaAliasNames := Seq(
    "it", "ops", "staging"
  )
) ++ apiGatewaySettings

lazy val root = (project in file(".")).
  enablePlugins(AWSApiGatewayPlugin).
  aggregate(hello).
  settings(commonSettings: _*).
  settings(awsSettings: _*).
  settings(apiGatewaySettings: _*)

lazy val hello = (project in file("./modules/hello")).
  enablePlugins(AWSServerlessPlugin).
  settings(commonSettings: _*).
  settings(assemblySettings: _*).
  settings(awsSettings: _*).
  settings(lambdaSettings: _*).
  settings(
    name := "Hello",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.1.0"
    ),
    awsLambdaHandler := "com.sample.Hello::handleRequest",
    awsApiGatewayResourcePath := "/hellos",
    awsApiGatewayResourceHttpMethod := "GET",
    awsApiGatewayIntegrationRequestTemplates := Seq(
      "application/json" -> """{"stage":{"env":"$stageVariables.env","region":"$stageVariables.region"},"company_id":"$input.params('company-id')","body":$input.json('$')}"""
    ),
    awsApiGatewayIntegrationResponseTemplates := ResponseTemplates(
      ResponseTemplate("200", None)
    )
  )

