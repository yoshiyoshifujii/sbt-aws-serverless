package com.github.yoshiyoshifujii.aws.serverless

import sbt._
import Keys._
import Def.Initialize

object Serverless {
  import sbtassembly.AssemblyPlugin.autoImport._

  def deployTask(key: TaskKey[Unit]): Initialize[Task[Unit]] = Def.task {
    println((assemblyOutputPath in key).value)
  }

}
