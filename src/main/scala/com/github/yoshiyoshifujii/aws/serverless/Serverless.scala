package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize
import com.github.yoshiyoshifujii.aws.serverless.keys.Deploy

object Serverless extends Deploy {
  import ServerlessPlugin.autoImport._

  def deployTask(key: TaskKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value
    val rootName = (name in key).value
    val rootDescription = (description in key).?.value
    val rootVersion = (version in key).?.value

    deployInvoke(so, rootName, rootDescription, rootVersion).get
  }

}
