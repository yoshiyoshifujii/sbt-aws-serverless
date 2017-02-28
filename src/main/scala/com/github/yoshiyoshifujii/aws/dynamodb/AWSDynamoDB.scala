package com.github.yoshiyoshifujii.aws.dynamodb

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.github.yoshiyoshifujii.aws.{AWSCredentials, AWSWrapper}
import com.github.yoshiyoshifujii.cliformatter.CliFormatter

import scala.util.Try

trait AWSDynamoDBWrapper extends AWSWrapper {
  val regionName: String

  lazy val client = {
    val c = new AmazonDynamoDBClient(AWSCredentials.provider)
    c.setRegion(RegionUtils.getRegion(regionName))
    c
  }

  def describeTable(tableName: String) = Try {
    client.describeTable(tableName)
  }

  def printTable(tableName: String) = {
    val p = describeTable(tableName) map { s =>
      CliFormatter(
        tableName,
        "Table ARN" -> 130,
        "Stream ARN" -> 130
      ).print2((
        s.getTable.getTableArn,
        s.getTable.getLatestStreamArn))
    } getOrElse {
      s"Not exists. $tableName"
    }
    println(p)
  }
}

case class AWSDynamoDB(regionName: String) extends AWSDynamoDBWrapper

