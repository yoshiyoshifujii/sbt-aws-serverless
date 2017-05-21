package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.util.Try

trait DeployListBase extends KeysBase {

  def invoke(stage: String): Try[Unit] = {
    lazy val generateArn: String => String =
      (fName) => lambda.generateLambdaArn(so.provider.awsAccount)(fName)(Some(stage))

    for {
      _ <- swap {
        for {
          ag <- so.apiGateway
          id <- ag.restApiId
        } yield
          for {
            _ <- api.printStages(id)
            _ <- api.printDeployments(id)
            _ <- api.printAuthorizers(id)
          } yield ()
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
        } yield
          for {
            _ <- s.printDescribe(so.provider.region, stage)
            _ <- lambda.printEventSourceMappings(generateArn(f.name))
            _ <- sequence {
              s.oldFunctions map { of =>
                lambda.printEventSourceMappings(generateArn(of.name))
              }
            }
          } yield ()
      }
    } yield ()
  }

}

case class DeployList(so: ServerlessOption) extends DeployListBase
