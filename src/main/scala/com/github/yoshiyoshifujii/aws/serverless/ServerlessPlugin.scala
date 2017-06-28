package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._

object ServerlessPlugin extends AutoPlugin {

  object autoImport extends ServerlessKeys {}

  import autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override def projectSettings: Seq[Def.Setting[_]] = serverlessSettings

  lazy val serverlessSettings: Seq[Def.Setting[_]] = Seq(
    serverlessDeploy := Serverless.deployTask(serverlessDeploy).evaluated,
    serverlessDeployDev := Serverless.deployDevTask(serverlessDeploy).evaluated,
    serverlessDeployFunction := Serverless.deployFunctionTask(serverlessDeploy).evaluated,
    serverlessDeployList := Serverless.deployListTask(serverlessDeploy).evaluated,
    serverlessInvoke := Serverless.invokeTask(serverlessDeploy).evaluated,
    serverlessInformation := Serverless.informationTask(serverlessDeploy).value,
    serverlessRemove := Serverless.removeTask(serverlessDeploy).value,
    serverlessRemoveStage := Serverless.removeStageTask(serverlessDeploy).evaluated,
    serverlessRemoveDeployment := Serverless.removeDeploymentTask(serverlessDeploy).evaluated,
    name in serverlessDeploy := name.value,
    description in serverlessDeploy := description.value,
    version in serverlessDeploy := version.value,
    serverlessOption in serverlessDeploy := serverlessOption.value,
    serverlessNoUploadMode in serverlessDeploy := serverlessNoUploadMode.?.value
      .getOrElse(false),
    serverlessDeployStream := Serverless.deployStreamTask(serverlessDeploy).evaluated,
    serverlessClean := Serverless.clean(serverlessDeploy).value
  )
}
