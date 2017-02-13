package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize
import complete.DefaultParsers._
import com.github.yoshiyoshifujii.aws.serverless.keys.{Deploy, DeployFunction}

object Serverless {

  import ServerlessPlugin.autoImport._

  def deployTask(key: TaskKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value
    val rootName = (name in key).value
    val rootDescription = (description in key).?.value
    val rootVersion = (version in key).?.value

    Deploy(so, rootName, rootDescription, rootVersion).invoke.get
  }

  def deployFunctionTask(key: TaskKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      functionName <- spaceDelimited("<functionName>").parsed match {
        case Seq(a) => Some(a)
        case _ => None
      }
      so = (serverlessOption in key).value
      function <- so.functions.find(functionName)
    } yield DeployFunction(so).deployFunction(function).get).getOrElse {
      sys.error("Error deployFunction. useage: deployFunction <functionName>")
    }
  }
}
