package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize

object Serverless {
  import sbtassembly.AssemblyPlugin.autoImport._
  import ServerlessPlugin.autoImport._

  def deployTask(key: TaskKey[Unit]): Initialize[Task[Unit]] = Def.task {
    println((assemblyOutputPath in key).value)
    println((serverless in key).value)
  }

}
