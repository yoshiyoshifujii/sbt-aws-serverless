package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._

object ServerlessPlugin extends AutoPlugin {

  object autoImport extends ServerlessKeys {

  }

  import autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override def projectSettings: Seq[Def.Setting[_]] = serverlessSettings

  lazy val serverlessSettings: Seq[Def.Setting[_]] = Seq(
    deploy := Serverless.deployTask(deploy).evaluated,
    deployDev := Serverless.deployDevTask(deploy).evaluated,
    deployFunction := Serverless.deployFunctionTask(deploy).evaluated,
    deployList := Serverless.deployListTask(deploy).evaluated,
    invoke := Serverless.invokeTask(deploy).evaluated,
    information := Serverless.informationTask(deploy).value,
    remove := Serverless.removeTask(deploy).value,
    removeDeployment := Serverless.removeDeploymentTask(deploy).evaluated,

    name in deploy := name.value,
    description in deploy := description.value,
    version in deploy := version.value,
    serverlessOption in deploy := serverlessOption.value
  )
}

