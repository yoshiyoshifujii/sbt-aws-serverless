import Dependencies._

lazy val root = (project in file(".")).
  settings(
    name := "sbt-aws-serverless",
    organization := "com.github.yoshiyoshifujii",
    sbtPlugin := true,
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3"),
    libraryDependencies ++= rootDeps
  )

lazy val sampleLambda = (project in file("./sample/lambda")).
  settings(
    name := "sampleScalaLambda",
    libraryDependencies ++= sampleLambdaDeps
  )

