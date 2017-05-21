package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.{ServerlessOption, Function => ServerlessFunction}

import scala.util.Try

trait DeployDevBase extends DeployBase {

  override protected def publishVersion(function: ServerlessFunction) =
    Try(None)

  override protected def generateFunctionVersion(publishedVersion: PublishedVersion) =
    Some("$LATEST")

}

case class DeployDev(so: ServerlessOption,
                     name: String,
                     description: Option[String],
                     version: Option[String],
                     noUploadMode: Boolean)
    extends DeployDevBase
