package com.github.yoshiyoshifujii.aws.serverless

import sbt._

object ServerlessPlugin extends AutoPlugin {

  object autoImport extends ServerlessKeys {

  }

  import autoImport._
  import sbtassembly.AssemblyPlugin.autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override def projectSettings: Seq[Def.Setting[_]] = serverlessSettings

  lazy val serverlessSettings: Seq[Def.Setting[_]] = Seq(

    deploy := Serverless.deployTask(deploy).value,

    assemblyOutputPath in deploy := (assemblyOutputPath in assembly).value

  )
}
