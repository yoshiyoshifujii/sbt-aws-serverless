package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait InformationBase extends KeysBase {

  val rootName: String

  def invoke: Try[Unit] = Try {
    import com.github.yoshiyoshifujii.aws.http._

    val endpoints = for {
      functions <- so.functions.filteredHttpEvents
      httpEvent <- functions.events.httpEventsMap(identity)
      ag        <- so.apiGateway
      restApiId <- ag.restApiId
    } yield {
      val url = generateUrl(
        region = so.provider.region,
        restApiId = restApiId,
        stageName = "$${stage}",
        path = httpEvent.path,
        pathWithQuerys = Seq.empty
      )
      s"  ${httpEvent.method} - $url"
    }

    val functions = so.functions.map(f => s"  ${f.nameWith("stage")}")

    println(s"""Service Information
               |service: $rootName
               |region: ${so.provider.region}
               |endpoints:
               |${endpoints.mkString("\n")}
               |functions:
               |${functions.mkString("\n")}
       """.stripMargin)
    ()
  }

}

case class Information(so: ServerlessOption, rootName: String) extends InformationBase
