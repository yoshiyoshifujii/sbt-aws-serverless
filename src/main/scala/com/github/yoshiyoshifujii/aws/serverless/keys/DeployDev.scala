package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.{ServerlessOption, Function => ServerlessFunction}

import scala.util.Try

trait DeployDevBase extends DeployBase {

  override protected def publishVersion(function: ServerlessFunction) =
    Try("")

  override protected def generateFunctionVersion(publishedVersion: String) =
    Some("$LATEST")

}

case class DeployDev(so: ServerlessOption,
                     name: String,
                     description: Option[String],
                     version: Option[String]) extends DeployDevBase

