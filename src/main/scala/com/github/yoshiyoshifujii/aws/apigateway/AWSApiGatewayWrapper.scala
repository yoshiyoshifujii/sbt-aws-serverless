package com.github.yoshiyoshifujii.aws.apigateway

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.apigateway.AmazonApiGatewayClient
import com.amazonaws.services.apigateway.model._
import com.github.yoshiyoshifujii.aws.AWSWrapper

trait AWSApiGatewayWrapper extends AWSWrapper {

  val regionName: String
  lazy val client = {
    val c = new AmazonApiGatewayClient()
    c.setRegion(RegionUtils.getRegion(regionName))
    c
  }

  protected def toOpt[A](f: => A) =
    try {
      Some(f)
    } catch {
      case e: NotFoundException => None
    }
}

