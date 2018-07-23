package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize
import com.github.yoshiyoshifujii.aws
import complete.DefaultParsers._
import serverless.FunctionBase

object Serverless {

  import ServerlessPlugin.autoImport._

  def deployTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
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
          .Deploy(so, rootName, rootDescription, rootVersion, noUploadMode)
          .invoke(stage)
          .get
      } yield ()).getOrElse {
        sys.error("Error serverlessDeploy. useage: serverlessDeploy <stage>")
      }
    }

  def deployDevTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
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
        sys.error("Error serverlessDeploy. useage: serverlessDeploy <stage>")
      }
    }

  def deployFunctionTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
      (for {
        (functionName, stage) <- spaceDelimited("<functionName> <stage>").parsed match {
          case Seq(a, b) => Some(a -> b)
          case _         => None
        }
        so           = (serverlessOption in key).value
        noUploadMode = (serverlessNoUploadMode in key).value
        function <- so.functions.find(functionName)
        _ = function match {
          case f: serverless.Function =>
            keys.DeployFunction(so, noUploadMode).invoke(f, stage).get
          case _ =>
            ""
        }
      } yield ()).getOrElse {
        sys.error(
          "Error serverlessDeployFunction. usage: serverlessDeployFunction <functionName> <stage>")
      }
    }

  def deployListTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
      (for {
        stage <- spaceDelimited("<stage>").parsed match {
          case Seq(a) => Some(a)
          case _      => None
        }
        so = (serverlessOption in key).value
        _  = keys.DeployList(so).invoke(stage).get
      } yield ()).getOrElse {
        sys.error("Error serverlessDeployList. usage: serverlessDeployList <stage>")
      }
    }

  def invokeTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
      (for {
        stage <- spaceDelimited("<stage>").parsed match {
          case Seq(a) => Some(a)
          case _      => None
        }
        so = (serverlessOption in key).value
        _  = keys.Invoke(so).invoke(stage).get
      } yield ()).getOrElse {
        sys.error("Error serverlessInvoke. usage: serverlessInvoke <stage>")
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

  def removeStageTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
      (for {
        stage <- spaceDelimited("<stage>").parsed match {
          case Seq(a) => Some(a)
          case _      => None
        }
        so = (serverlessOption in key).value
        _ = aws.? {
          keys.RemoveStage(so).invoke(stage).get
        }
      } yield ()).getOrElse {
        sys.error("Error serverlessRemoveStage. usage: serverlessRemoveStage <stage>")
      }
    }

  def removeDeploymentTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
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
        sys.error(
          "Error serverlessRemoveDeployment. usage: serverlessRemoveDeployment <deploymentId>")
      }
    }

  def deployStreamTask(key: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
      (for {
        stage <- spaceDelimited("<stage>").parsed match {
          case Seq(a) => Some(a)
          case _      => None
        }
        so = (serverlessOption in key).value
        _  = keys.DeployStream(so).invoke(stage).get
      } yield ()).getOrElse {
        sys.error("Error serverlessDeployList. usage: serverlessDeployList <stage>")
      }
    }

  def clean(key: InputKey[Unit]): Initialize[Task[Unit]] = Def.task {
    val so = (serverlessOption in key).value
    keys.Clean(so).invoke.get
  }

  def functionsDeployTask(deployKey: InputKey[Unit], functionsDeployKey: InputKey[Unit]): Initialize[InputTask[Unit]] =
    Def.inputTask {
      (for {
        stage <- spaceDelimited("<stage>").parsed match {
          case Seq(a) => Some(a)
          case _      => None
        }
        so              = (serverlessOption in deployKey).value
        rootName        = (name in deployKey).value
        rootDescription = (description in deployKey).?.value
        rootVersion     = (version in deployKey).?.value
        noUploadMode    = (serverlessNoUploadMode in deployKey).value
        functionNames   = (serverlessFunctionNames in functionsDeployKey).value
        functions <- functionNames.foldLeft(Option(Seq.empty: Seq[serverless.Function]))(
          (acc, functionName) =>
            acc match {
              case None => None
              case Some(_) => {
                so.functions.find(functionName) match {
                  case Some(a: serverless.Function) => acc.map(x => x ++ Seq(a))
                  case _                            => None
                }
              }
          })
        _ = keys
          .FunctionsDeploy(so, rootName, rootDescription, rootVersion, noUploadMode)
          .invokeFunctionsDeploy(functions, stage)
          .get
      } yield ()).getOrElse {
        sys.error(
          "Error serverlessFunctionDeploy. useage: set serverlessFunctionNames := Seq(\"<functionName>\"); serverlessFunctionDeploy <stage>")
      }
    }

}
