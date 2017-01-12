package com.github.yoshiyoshifujii.aws.serverless

import sbt._

trait ServerlessKeys {

  lazy val deploy         = taskKey[Unit]("")

  lazy val deployFunction = taskKey[Unit]("")

  lazy val deployList     = taskKey[Unit]("")

  lazy val invoke         = taskKey[Unit]("")

  lazy val logs           = taskKey[Unit]("")

  lazy val info           = taskKey[Unit]("")

  lazy val remove         = taskKey[Unit]("")

}

object ServerlessKeys extends ServerlessKeys

