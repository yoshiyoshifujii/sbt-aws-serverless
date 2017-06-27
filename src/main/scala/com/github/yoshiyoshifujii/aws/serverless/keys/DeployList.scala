package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.ServerlessOption

import scala.collection.JavaConverters._
import scala.util.Try

trait DeployListBase extends KeysBase {

  private def format(v: String) = s"- $v"

  private def doPrint(key: String, values: Any*) = {
    println(s"$key:")
    values foreach {
      case s: String       => println(format(s))
      case s: Long         => println(format(s.toString))
      case Some(s: String) => println(format(s))
      case Some(s: Long)   => println(format(s.toString))
    }
  }

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
            stages      <- api.getStages(id)
            deployments <- api.getDeployments(id)
            authorizers <- api.getAuthorizers(id)
          } yield {
            val deploymentMap = deployments.getItems.asScala.map(i => i.getId -> i).toMap
            stages.getItem.asScala.filter(_.getStageName == stage).foreach { s =>
              doPrint("Stage", s.getStageName)
              doPrint("Deployment",
                      s.getDeploymentId,
                      deploymentMap.get(s.getDeploymentId).map(_.getDescription))
            }
            authorizers.getItems.asScala.foreach { a =>
              doPrint("Authorize", a.getId, a.getName, a.getType, a.getAuthType)
            }
          }
      }
      _ <- sequence {
        so.functions.map { f =>
          for {
            aliases  <- lambda.listAliases(f.name)
            versions <- lambda.listVersionsByFunction(f.name)
          } yield {
            val versionMap = versions.getVersions.asScala.map(i => i.getVersion -> i).toMap
            aliases.getAliases.asScala.filter(_.getName.contains(stage)).foreach { a =>
              val v = versionMap.get(a.getFunctionVersion)
              doPrint(
                "Lambda",
                f.name,
                a.getName,
                a.getFunctionVersion,
                a.getDescription,
                v.map(_.getLastModified),
                v.map(_.getCodeSize)
              )
            }
          }
        }
      }
      _ <- sequence {
        for {
          f <- so.functions.filteredStreamEvents
          s <- f.events.streamEvents
        } yield
          for {
            _            <- s.printDescribe(so.provider.region, stage)
            eventSources <- lambda.listEventSourceMappings(generateArn(f.name))
            oldEventSources <- sequence {
              s.oldFunctions map { of =>
                lambda.listEventSourceMappings(generateArn(of.name))
              }
            }.map(_.flatMap(_.getEventSourceMappings.asScala))
          } yield {
            eventSources.getEventSourceMappings.asScala.foreach { es =>
              doPrint("EventSources",
                      es.getFunctionArn,
                      es.getUUID,
                      es.getState,
                      es.getLastModified.toString)
            }
            oldEventSources.foreach { oes =>
              doPrint("OldEventSources",
                      oes.getFunctionArn,
                      oes.getUUID,
                      oes.getState,
                      oes.getLastModified.toString)
            }
          }
      }
    } yield ()
  }

}

case class DeployList(so: ServerlessOption) extends DeployListBase
