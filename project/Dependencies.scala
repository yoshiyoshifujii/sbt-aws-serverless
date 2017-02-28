import sbt._

object Dependencies {

  val awsSdkVersion = "1.11.76"

  val awsJavaSdkLambda      = "com.amazonaws" % "aws-java-sdk-lambda"      % awsSdkVersion
  val awsJavaSdkApiGateway  = "com.amazonaws" % "aws-java-sdk-api-gateway" % awsSdkVersion
  val awsJavaSdkS3          = "com.amazonaws" % "aws-java-sdk-s3"          % awsSdkVersion
  val awsJavaSdkKinesis     = "com.amazonaws" % "aws-java-sdk-kinesis"     % awsSdkVersion
  val awsJavaSdkDynamoDB    = "com.amazonaws" % "aws-java-sdk-dynamodb"    % awsSdkVersion

  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.4"

  // Typesafe Config
  val config = "com.typesafe" % "config" % "1.3.0"

  lazy val rootDeps = Seq(
    awsJavaSdkLambda,
    awsJavaSdkApiGateway,
    awsJavaSdkS3,
    awsJavaSdkKinesis,
    awsJavaSdkDynamoDB,
    config % Test,
    scalaTest % Test
  )

}

