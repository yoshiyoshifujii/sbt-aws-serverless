package com.github.yoshiyoshifujii.aws.kinesis

import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.model.DescribeStreamRequest
import com.github.yoshiyoshifujii.aws.{AWSCredentials, AWSWrapper}
import com.github.yoshiyoshifujii.cliformatter.CliFormatter

import scala.util.Try

trait AWSKinesisWrapper extends AWSWrapper {
  val regionName: String

  lazy val client = AmazonKinesisClientBuilder.standard()
    .withCredentials(AWSCredentials.provider)
    .withRegion(regionName)
    .build()

  def describeStream(streamName: StreamName) = Try {
    val request = new DescribeStreamRequest()
      .withStreamName(streamName)

    client.describeStream(request)
  }

  def printDescribeStream(streamName: StreamName) = {
    val p = describeStream(streamName) map { s =>
      CliFormatter(
        streamName,
        "Stream ARN" -> 130,
        "Status" -> 10
      ).print2((
        s.getStreamDescription.getStreamARN,
        s.getStreamDescription.getStreamStatus))
    } getOrElse {
      s"Not exists. $streamName"
    }
    println(p)
  }

}

case class AWSKinesis(regionName: String) extends AWSKinesisWrapper

