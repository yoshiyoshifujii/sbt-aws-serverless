package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize
import com.github.yoshiyoshifujii.aws.apigateway.AWSApiGatewayRestApi

import scala.collection.JavaConverters._
import scala.util.Try

object Serverless {
  import sbtassembly.AssemblyPlugin.autoImport._
  import ServerlessPlugin.autoImport._

  def deployTask(key: TaskKey[Unit]): Initialize[Task[Unit]] = Def.task {
    println((assemblyOutputPath in key).value)
    println((serverlessOption in key).value)
    println(name.value)


  }

}
