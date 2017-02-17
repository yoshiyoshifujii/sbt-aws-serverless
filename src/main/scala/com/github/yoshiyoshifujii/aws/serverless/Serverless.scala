package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize
import complete.DefaultParsers._

object Serverless {

  import ServerlessPlugin.autoImport._

  def deployTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _ => None
      }
      so = (serverlessOption in key).value
      rootName = (name in key).value
      rootDescription = (description in key).?.value
      rootVersion = (version in key).?.value
      _ = keys.Deploy(so, rootName, rootDescription, rootVersion).invoke(stage).get
    } yield ()).getOrElse {
      sys.error("Error deploy. useage: deploy <stage>")
    }
  }

  def deployDevTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _ => None
      }
      so = (serverlessOption in key).value
      rootName = (name in key).value
      rootDescription = (description in key).?.value
      rootVersion = (version in key).?.value
      _ = keys.DeployDev(so, rootName, rootDescription, rootVersion).invoke(stage).get
    } yield ()).getOrElse {
      sys.error("Error deploy. useage: deploy <stage>")
    }
  }

  def deployFunctionTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      functionName <- spaceDelimited("<functionName>").parsed match {
        case Seq(a) => Some(a)
        case _ => None
      }
      so = (serverlessOption in key).value
      function <- so.functions.find(functionName)
      _ = keys.DeployFunction(so).invoke(function).get
    } yield ()).getOrElse {
      sys.error("Error deployFunction. useage: deployFunction <functionName>")
    }
  }

  def deployListTask(key: InputKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value

    keys.DeployList(so).invoke.get
  }

  def invokeTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _ => None
      }
      so = (serverlessOption in key).value
      _ = keys.Invoke(so).invoke(stage).get
    } yield ()).getOrElse {
      sys.error("Error invoke. useage: invoke <stage>")
    }
  }

  def informationTask(key: InputKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value
    val rootName = (name in key).value

    keys.Information(so, rootName).invoke.get
  }

  def remove(key: InputKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value

    keys.Remove(so).invoke.get
  }

}
