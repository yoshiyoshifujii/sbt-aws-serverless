package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize
import com.github.yoshiyoshifujii.aws
import complete.DefaultParsers._

object Serverless {

  import ServerlessPlugin.autoImport._

  def deployTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _      => None
      }
      so              = (serverlessOption in key).value
      rootName        = (name in key).value
      rootDescription = (description in key).?.value
      rootVersion     = (version in key).?.value
      noUploadMode    = (serverlessNoUploadMode in key).value
      _               = keys.Deploy(so, rootName, rootDescription, rootVersion, noUploadMode).invoke(stage).get
    } yield ()).getOrElse {
      sys.error("Error deploy. useage: deploy <stage>")
    }
  }

  def deployCopyTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      (from, to) <- spaceDelimited("<from stage> <to stage>").parsed match {
        case Seq(a, b) => Some(a -> b)
        case _         => None
      }
      so          = (serverlessOption in key).value
      rootVersion = (version in key).?.value
      _           = keys.DeployCopy(so, rootVersion).invoke(from, to).get
    } yield ()).getOrElse {
      sys.error("Error deployCopy. useage: deployCopy <from stage> <to stage>")
    }
  }

  def deployDevTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _      => None
      }
      so              = (serverlessOption in key).value
      rootName        = (name in key).value
      rootDescription = (description in key).?.value
      rootVersion     = (version in key).?.value
      noUploadMode    = (serverlessNoUploadMode in key).value
      _ = keys
        .DeployDev(so, rootName, rootDescription, rootVersion, noUploadMode)
        .invoke(stage)
        .get
    } yield ()).getOrElse {
      sys.error("Error deploy. useage: deploy <stage>")
    }
  }

  def deployFunctionTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      (functionName, stageOpt) <- spaceDelimited("<functionName> [stage]").parsed match {
        case Seq(a, b) => Some(a -> Some(b))
        case Seq(a)    => Some(a -> None)
        case _         => None
      }
      so           = (serverlessOption in key).value
      noUploadMode = (serverlessNoUploadMode in key).value
      function <- so.functions.find(functionName)
      _ = function match {
        case f: serverless.Function =>
          keys.DeployFunction(so, noUploadMode).invoke(f, stageOpt).get
        case _ =>
          ""
      }
    } yield ()).getOrElse {
      sys.error("Error deployFunction. useage: deployFunction <functionName>")
    }
  }

  def deployListTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _      => None
      }
      so = (serverlessOption in key).value
      _  = keys.DeployList(so).invoke(stage).get
    } yield ()).getOrElse {
      sys.error("Error deployList. useage: deployList <stage>")
    }
  }

  def invokeTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _      => None
      }
      so = (serverlessOption in key).value
      _  = keys.Invoke(so).invoke(stage).get
    } yield ()).getOrElse {
      sys.error("Error invoke. useage: invoke <stage>")
    }
  }

  def informationTask(key: InputKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so       = (serverlessOption in key).value
    val rootName = (name in key).value

    keys.Information(so, rootName).invoke.get
  }

  def removeTask(key: InputKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value

    aws.? {
      keys.Remove(so).invoke.get
    }
  }

  def removeDeploymentTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      deploymentId <- spaceDelimited("<deploymentId>").parsed match {
        case Seq(a) => Some(a)
        case _      => None
      }
      so = (serverlessOption in key).value
      _ = aws.? {
        keys.RemoveDeployment(so).invoke(deploymentId).get
      }
    } yield ()).getOrElse {
      sys.error("Error removeDeployment. useage: removeDeployment <deploymentId>")
    }
  }

  def deployStreamTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _      => None
      }
      so = (serverlessOption in key).value
      _  = keys.DeployStream(so).invoke(stage).get
    } yield ()).getOrElse {
      sys.error("Error deployList. useage: deployList <stage>")
    }
  }

  def clean(key: InputKey[Unit]): Initialize[InputTask[Unit]] = Def.inputTask {
    (for {
      stage <- spaceDelimited("<stage>").parsed match {
        case Seq(a) => Some(a)
        case _      => None
      }
      so = (serverlessOption in key).value
      _  = keys.Clean(so).invoke(stage).get
    } yield ()).getOrElse {
      sys.error("Error serverlessClean. useage: serverlessClean <stage>")
    }
  }

}
