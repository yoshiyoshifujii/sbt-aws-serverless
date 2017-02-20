package com.github.yoshiyoshifujii.aws.serverless.keys

import com.github.yoshiyoshifujii.aws.kinesis.AWSKinesis
import serverless.ServerlessOption

import scala.util.Try

trait DeployListBase extends KeysBase {

  def invoke(stage: String): Try[Unit] =
    for {
      _ <- swap {
        so.provider.restApiId map { id =>
          for {
            _ <- api.printStages(id)
            _ <- api.printDeployments(id)
            _ <- api.printAuthorizers(id)
          } yield ()
        }
      }
      _ <- sequence {
        so.functions.map { f =>
          for {
            _ <- lambda.printListVersionsByFunction(f.name)
            _ <- lambda.printListAliases(f.name)
          } yield ()
        }
      }
      _ <- sequence {
        for {
          f <- so.functions.filteredStreamEvents
          s <- f.events.streamEvents
        } yield for {
          _ <- Try {
            AWSKinesis(so.provider.region).printDescribeStream(s.appendToTheNameSuffix(stage))
          }
          _ <- {
            val functionArn =
              lambda.generateLambdaArn(so.provider.awsAccount)(f.name)(Some(stage))
            lambda.printEventSourceMappings(functionArn)
          }
        } yield ()
      }
    } yield ()

}

case class DeployList(so: ServerlessOption) extends DeployListBase

