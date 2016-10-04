package com.github.yoshiyoshifujii.aws

package object kinesis {

  type Region = String
  type AWSAccount = String
  type StreamName = String

  lazy val generateKinesisStreamArn =
    (regionName: Region) =>
      (awsAccount: AWSAccount) =>
        (streamName: StreamName) =>
          s"arn:aws:kinesis:$regionName:$awsAccount:stream/$streamName"

}
