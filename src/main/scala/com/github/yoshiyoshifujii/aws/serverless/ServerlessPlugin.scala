package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._

object ServerlessPlugin extends AutoPlugin {

  object autoImport extends ServerlessKeys {

  }

  import autoImport._
  import sbtassembly.AssemblyPlugin.autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override def projectSettings: Seq[Def.Setting[_]] = serverlessSettings

  lazy val serverlessSettings: Seq[Def.Setting[_]] = Seq(
    deploy := Serverless.deployTask(deploy).evaluated,
    deployDev := Serverless.deployDevTask(deploy).evaluated,
    deployFunction := Serverless.deployFunctionTask(deploy).evaluated,
    deployList := Serverless.deployListTask(deploy).value,
    invoke := Serverless.invokeTask(deploy).evaluated,
    remove := Serverless.remove(deploy).value,

    name in deploy := name.value,
    description in deploy := description.value,
    version in deploy := version.value,
    assemblyOutputPath in deploy := (assemblyOutputPath in assembly).value,
    serverlessOption in deploy := serverlessOption.value
  )
}

