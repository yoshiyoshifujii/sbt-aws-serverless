package com.github.yoshiyoshifujii.aws.serverless

import sbt._

object ServerlessPlugin extends AutoPlugin {

  object autoImport extends ServerlessKeys {

  }

  import autoImport._
  import sbtassembly.AssemblyPlugin.autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override def projectSettings: Seq[Def.Setting[_]] = serverlessSettings

  lazy val serverlessSettings: Seq[Def.Setting[_]] = Seq(
    deploy := Serverless.deployTask(deploy).value,
    assemblyOutputPath in deploy := (assemblyOutputPath in assembly).value,
    serverless := {
      ServerlessOption(
        Provider(
          awsAccount = "",
          deploymentBucket = "hogehoge"
        ),
        Functions(
          Function(
            name = "hoge",
            handler = "",
            role = "",
            events = Events(
              HttpEvent(
                path = "/hoge",
                method = "GET"
              )
            )
          )
        )
      )
    },
    serverless in deploy := serverless.value
  )
}

case class Provider(awsAccount: String,
                    stage: String = "dev",
                    region: String = "us-east-1",
                    deploymentBucket: String)

case class Authorizer(name: String,
                      arn: String,
                      resultTtlInSeconds: Int,
                      identitySourceHeaderName: String = "Authorization",
                      identityValidationExpression: String)

trait Event

case class HttpEvent(path: String,
                     method: String,
                     uriLambdaAlias: String = "${stageVariables.env}",
                     cors: Boolean = false,
                     `private`: Boolean = false,
                     authorizer: Authorizer = null) extends Event

case class StreamEvent(arn: String,
                       batchSize: Int = 100,
                       startingPosition: String,
                       enabled: Boolean = false) extends Event

case class Events(events: Event*)

case class Function(name: String,
                    description: Option[String] = None,
                    handler: String,
                    memorySize: Int = 512,
                    timeout: Int = 10,
                    role: String,
                    environment: Map[String, String] = Map.empty,
                    events: Events)

case class Functions(functions: Function*)

case class ServerlessOption(provider: Provider,
                            functions: Functions)

