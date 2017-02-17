package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait DeployDevBase extends DeployBase {

}

case class DeployDev(so: ServerlessOption,
                     name: String,
                     description: Option[String],
                     version: Option[String]) extends DeployDevBase

