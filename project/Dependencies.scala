import sbt._

object Dependencies {

  val awsSdkVersion = "1.11.29"

  val awsJavaSdkLambda      = "com.amazonaws" % "aws-java-sdk-lambda"       % awsSdkVersion
  val awsJavaSdkApiGateway  = "com.amazonaws" % "aws-java-sdk-api-gateway"  % awsSdkVersion
  val awsJavaSdkS3          = "com.amazonaws" % "aws-java-sdk-s3"           % awsSdkVersion
  val awsJavaSdkKinesis     = "com.amazonaws" % "aws-java-sdk-kinesis"      % awsSdkVersion

  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.4"

  // Amazon Lambda
  val awsLambdaJavaCore = "com.amazonaws" % "aws-lambda-java-core" % "1.1.0"

  // Typesafe Config
  val config = "com.typesafe" % "config" % "1.3.0"

  lazy val rootDeps = Seq(
    awsJavaSdkLambda,
    awsJavaSdkApiGateway,
    awsJavaSdkS3,
    awsJavaSdkKinesis,
    config % Test,
    scalaTest % Test
  )

  lazy val sampleLambdaDeps = Seq(
    awsLambdaJavaCore
  )

}

