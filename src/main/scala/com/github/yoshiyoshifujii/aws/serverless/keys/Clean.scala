package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.{Deployment, GetDeploymentsResult, GetStagesResult}
import com.amazonaws.services.lambda.model.{FunctionConfiguration, ListVersionsByFunctionResult}
import com.github.yoshiyoshifujii.aws.apigateway.RestApiId
import serverless.FunctionBase

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait CleanBase extends KeysBase {

  private def getFunctionArns(restApiId: RestApiId, stages: GetStagesResult): Try[Seq[String]] =
    sequence {
      stages.getItem.asScala map { stage =>
        val stageName      = stage.getStageName
        val stageVariables = stage.getVariables.asScala.toMap
        api.exportFunctionArns(restApiId, stageName, stageVariables)
      }
    }.map(_.flatten.distinct.sorted)

  private case class FunctionAndPublished(
      private val functionBase: FunctionBase,
      private val functionConfiguration: FunctionConfiguration) {
    val name         = functionConfiguration.getFunctionName
    val aliasVersion = functionConfiguration.getVersion
    val publishedArn = functionConfiguration.getFunctionArn
  }

  private case class FunctionAndListVersionsResult(
      private val functionBase: FunctionBase,
      private val listVersionsByFunctionResult: ListVersionsByFunctionResult) {
    val functionAndPublished =
      listVersionsByFunctionResult.getVersions.asScala map {
        FunctionAndPublished(functionBase, _)
      }
  }

  private def getHttpEventPublishes(stages: GetStagesResult): Try[Seq[FunctionAndPublished]] =
    sequence {
      for {
        stage <- stages.getItem.asScala.map(_.getStageName)
        func  <- so.functions.filteredHttpEvents
        if {
          lambda.get(func.nameWith(stage)) match {
            case Success(s) => s.isDefined
            case Failure(_) => false
          }
        }
      } yield
        for {
          result <- lambda
            .listVersionsByFunction(func.nameWith(stage))
            .map(FunctionAndListVersionsResult(func, _))
        } yield result
    }.map(_.flatMap(_.functionAndPublished))

  private def getStreamEventPublishes(stages: GetStagesResult): Try[Seq[FunctionAndPublished]] =
    sequence {
      for {
        stage <- stages.getItem.asScala.map(_.getStageName)
        func  <- so.functions.filteredStreamEvents
      } yield
        lambda
          .listVersionsByFunction(func.nameWith(stage))
          .map(FunctionAndListVersionsResult(func, _))
    }.map(_.flatMap(_.functionAndPublished))

  private def getStreamEventVersions(stages: GetStagesResult): Try[Seq[FunctionConfiguration]] =
    sequence {
      for {
        stage <- stages.getItem.asScala.map(_.getStageName)
        func  <- so.functions.filteredStreamEvents
      } yield lambda.listVersionsByFunction(func.nameWith(stage))
    }.map(_.flatMap(_.getVersions.asScala))

  private def skip[E](t: Try[E]): Try[Unit] =
    t match {
      case Success(_) => Try(())
      case Failure(e) =>
        println(e.getMessage)
        Try(())
    }

  private def deletePublishes(exportFunctionArns: Seq[String],
                              publishes: Seq[FunctionAndPublished]): Try[Unit] = {
    val publishedMap      = publishes.map(a => a.publishedArn -> a).toMap
    val notDeletion       = exportFunctionArns flatMap publishedMap.get
    val deletionCandidate = publishes diff notDeletion
    for {
      _ <- sequence {
        deletionCandidate
          .filter(_.aliasVersion != "$LATEST")
          .map(_.publishedArn)
          .distinct
          .sorted
          .map { arn =>
            skip {
              lambda.delete(arn)
            }
          }
      }
    } yield ()
  }

  private def deleteDeployments(restApiId: RestApiId,
                                stages: GetStagesResult,
                                deployments: Seq[Deployment]): Try[Unit] = {
    val usedDeploymentIds   = stages.getItem.asScala.map(_.getDeploymentId)
    val unUsedDeploymentIds = deployments.map(_.getId) diff usedDeploymentIds
    for {
      _ <- sequence {
        unUsedDeploymentIds map { id =>
          skip {
            api.deleteDeployment(restApiId, id)
          }
        }
      }
    } yield ()
  }

  private def cleanApi(restApiId: RestApiId, stages: GetStagesResult) =
    for {
      exportFunctionArns <- getFunctionArns(restApiId, stages)
      aliases            <- getHttpEventPublishes(stages)
      _                  <- deletePublishes(exportFunctionArns, aliases)
      deployments        <- api.getDeployments(restApiId)
      _                  <- deleteDeployments(restApiId, stages, deployments)
    } yield ()

  def deleteNoUseVersion(versions: Seq[FunctionConfiguration], aliases: Seq[FunctionAndPublished]) =
    Try {
      val deletionCandidate = versions.map(_.getFunctionArn) diff aliases.map(_.publishedArn)
      deletionCandidate map { arn =>
        skip {
          lambda.delete(arn)
        }
      }
    }

  private def cleanStream(stages: GetStagesResult) =
    for {
      aliases  <- getStreamEventPublishes(stages)
      versions <- getStreamEventVersions(stages)
      _        <- deleteNoUseVersion(versions, aliases)
    } yield ()

  def invoke: Try[Unit] =
    swap {
      so.restApiId map { restApiId =>
        for {
          stages <- api.getStages(restApiId)
          _      <- Try(println("clean api"))
          _      <- cleanApi(restApiId, stages)
          _      <- Try(println("clean stream"))
          _      <- cleanStream(stages)
        } yield {}
      }
    }.map(_ => ())
}

case class Clean(so: serverless.ServerlessOption) extends CleanBase
