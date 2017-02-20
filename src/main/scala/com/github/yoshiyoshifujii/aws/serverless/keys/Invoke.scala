package com.github.yoshiyoshifujii.aws.serverless.keys

import java.io.ByteArrayOutputStream

import com.github.yoshiyoshifujii.aws.http._
import serverless.ServerlessOption

import scala.util.Try

trait InvokeBase extends KeysBase {

  def invoke(stage: String): Try[Seq[Unit]] = {
    sequence {
      for {
        function  <- so.functions.filteredHttpEvents
        httpEvent <- function.events.httpEventsMap(identity)
        input     <- httpEvent.invokeInput
        restApiId <- so.provider.restApiId
      } yield {
        val url = generateUrl(
          region = so.provider.region,
          restApiId = restApiId,
          stageName = stage,
          path = httpEvent.path,
          pathWithQuerys = input.pathWithQuerys
        )
        for {
          response <- request(
            url = url,
            method = httpEvent.method,
            headers = input.headers,
            parameters = input.parameters,
            body = input.body
          )
        } yield {
          val out = new ByteArrayOutputStream()
          (for {
            r <- Option(response)
            e <- Option(r.getEntity)
            s <- Option(r.getStatusLine)
          } yield {
            e.writeTo(out)
            println(
              s"""============================================================
                 |${httpEvent.method}:$url
                 |============================================================
                 |${s.getStatusCode}
                 |${out.toString("utf-8")}
           """.stripMargin)
          }) getOrElse ()
        }
      }
    }
  }

}

case class Invoke(so: ServerlessOption) extends InvokeBase

