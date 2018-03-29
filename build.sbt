import Dependencies._

val sbtCrossVersion = sbtVersion in pluginCrossBuild

lazy val root = (project in file(".")).settings(
  name := "sbt-aws-serverless",
  organization := "com.github.yoshiyoshifujii",
  sbtPlugin := true,
  scalaVersion := (CrossVersion partialVersion sbtCrossVersion.value match {
    case Some((0, 13)) => "2.10.6"
    case Some((1, _))  => "2.12.4"
    case _             => sys error s"Unhandled sbt version ${sbtCrossVersion.value}"
  }),
  addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6"),
  libraryDependencies ++= rootDeps
)
