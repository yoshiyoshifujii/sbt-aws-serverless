package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.{FunctionBase, ServerlessOption, StreamEvent}

import scala.util.Try

trait DeployStreamBase extends KeysBase {

  private lazy val delete: (String) => (Seq[FunctionBase]) => ((String) => String) => Try[Seq[_]] =
    (stage: String) =>
      (oldFunctions: Seq[FunctionBase]) =>
        (generateArn: String => String) =>
          sequence {
            oldFunctions map { f =>
              val arn = generateArn(f.nameWith(stage))
              lambda.deleteEventSourceMappings(arn)
            }
    }

  protected def deployStream(stage: String, function: FunctionBase, streamEvent: StreamEvent) = {
    lazy val generateArn: String => String =
      lambda.generateLambdaArn(so.provider.awsAccount)

    val functionArn = generateArn(function.nameWith(stage))

    for {
      _              <- lambda.deleteEventSourceMappings(functionArn)
      _              <- delete(stage)(streamEvent.oldFunctions)(generateArn)
      eventSourceArn <- streamEvent.getArn(so.provider.region, stage)
      c <- lambda.createEventSourceMapping(
        functionArn = functionArn,
        eventSourceArn = eventSourceArn,
        enabled = streamEvent.enabled,
        batchSize = streamEvent.batchSize,
        startPosition = streamEvent.startingPosition.value
      )
      _ = { println(s"Event Source Mapping: ${c.toString}") }
    } yield c
  }

}

case class DeployStream(so: ServerlessOption) extends DeployStreamBase {

  def invoke(stage: String): Try[Unit] =
    for {
      _ <- sequence {
        for {
          f <- so.functions.filteredStreamEvents
          e <- f.events.streamEvents
        } yield
          deployStream(
            stage = stage,
            function = f,
            streamEvent = e
          )
      }
    } yield ()

}
