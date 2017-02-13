package com.github.yoshiyoshifujii.aws.serverless.keys

import com.github.yoshiyoshifujii.aws.lambda.AWSLambda
import serverless.ServerlessOption

trait KeysBase {

  val so: ServerlessOption

  lazy protected val lambda = AWSLambda(so.provider.region)

}
