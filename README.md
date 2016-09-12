# sbt-aws-serverless

sbt plugin to deploy code to Amazon API Gateway and AWS Lambda

## Installation

Add the following to your `project/plugins.sbt` file:

```sbt
lazy val root = project.in(file(".")).dependsOn(githubRepo)

lazy val githubRepo = uri("git://github.com/yoshiyoshifujii/sbt-aws-serverless.git#master")
```

Add the `AWSApiGatewayPlugin` auto-plugin to your build.sbt:

```sbt
enablePlugins(AWSApiGatewayPlugin)
```

Add the `AWSServerlessPlugin` auto-plugin to your build.sbt:

```sbt
enablePlugins(AWSServerlessPlugin)
```

## Usage

`sbt createApiGateway [name]` creates a new Amazon API Gateway Rest API.

`sbt putApiGateway` put a swagger.yaml to Amazon API Gateway Rest API.

`sbt deploy` deploy AWS Lambda function from the current project.

`sbt deployStages` deploy the Amazon API Gateway Stages.

## Configuration

### AWSApiGatewayPlugin

| sbt setting                   | Description                                               |
|-------------------------------|-----------------------------------------------------------|
| awsRegion                     | Amazon API Gateway and AWS Lambda region.                 |
| awsAccountId                  | Amazon Account ID.                                        |
| awsApiGatewayRestApiId        | The identifier of the RestApi to be updated.              |
| awsApiGatewayYAMLFile         | The PUT request body containing external API definitions. |
| awsApiGatewayStages           | Stage name and stage's description.                       |
| awsApiGatewayStageVariables   | A map that defines the stage variables.                   |

### AWSServerlessPlugin

| sbt setting                               | Description                                                                 |
|-------------------------------------------|-----------------------------------------------------------------------------|
| awsLambdaFunctionName                     | The name you want to assign to the function you are uploading.              |
| awsLambdaDescription                      | A short, user-defined function description.                                 |
| awsLambdaHandler                          | The function within your code that Lambda calls to begin execution.         |
| awsLambdaRole                             | The Amazon Resource Name (ARN) of the IAM role.                             |
| awsLambdaTimeout                          | The function execution time at which Lambda should terminate the function.  |
| awsLambdaMemorySize                       | The amount of memory, in MB, your Lambda function is given.                 |
| awsLambdaS3Bucket                         | The name of an S3 bucket where the lambda code will be stored.              |
| awsLambdaDeployDescription                | The description for the version you are publishing.                         |
| awsLambdaAliasNames                       | Name for the alias you are creating.                                        |
| awsApiGatewayResourcePath                 | The full path for this resource.                                            |
| awsApiGatewayResourceHttpMethod           | Specifies a put integration request's HTTP method.                          |
| awsApiGatewayResourceUriLambdaAlias       | Specifies a put integration input's Uniform Resource Identifier (URI).      |
| awsApiGatewayIntegrationRequestTemplates  | Represents a map of Velocity templates.                                     |
| awsApiGatewayIntegrationResponseTemplates | Specifies a put integration response's templates.                           |

An example configuration might look like this:

```sbt
import com.github.yoshiyoshifujii.aws.apigateway._

val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8",
  organization := "com.github.yoshiyoshifujii.sample-serverless"
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
  awsAccountId := "<AWS Account ID>"
)

val apiGatewaySettings = Seq(
  awsApiGatewayRestApiId := "<REST API ID>",
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
  awsLambdaRole := "<ROLE ARN>",
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
```

