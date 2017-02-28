package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.{FunctionBase, StreamEvent}

trait DeployStream extends KeysBase {

  protected def deployStream(stage: String,
                             function: FunctionBase,
                             streamEvent: StreamEvent) = {
    val functionArn = lambda.generateLambdaArn(so.provider.awsAccount)(function.name)(Some(stage))

    for {
      _ <- lambda.deleteEventSourceMappings(functionArn)
      eventSourceArn <- streamEvent.getArn(so.provider.region, stage)
      c <- lambda.createEventSourceMapping(
        functionArn = functionArn,
        eventSourceArn = eventSourceArn,
        batchSize = streamEvent.batchSize,
        startPosition = streamEvent.startingPosition.value
      )
      _ = { println(s"Event Source Mapping: ${c.toString}") }
    } yield c
  }

}
