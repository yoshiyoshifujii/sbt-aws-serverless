package com.github.yoshiyoshifujii.aws.apigateway

import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder
import com.amazonaws.services.apigateway.model._
import com.github.yoshiyoshifujii.aws.{AWSCredentials, AWSWrapper}

trait AWSApiGatewayWrapper extends AWSWrapper {

  val regionName: String
  lazy val client = AmazonApiGatewayClientBuilder.standard()
    .withCredentials(AWSCredentials.provider)
    .withRegion(regionName)
    .build()

  protected def toOpt[A](f: => A) =
    try {
      Some(f)
    } catch {
      case e: NotFoundException => None
    }
}

