package com.github.yoshiyoshifujii.aws.serverless.keys

import com.amazonaws.services.apigateway.model.{GetDeploymentsResult, GetStagesResult}
import com.amazonaws.services.lambda.model.{
  AliasConfiguration,
  FunctionConfiguration,
  ListAliasesResult
}
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

  private case class FunctionAndAlias(private val functionBase: FunctionBase,
                                      private val aliasConfiguration: AliasConfiguration) {
    val name         = functionBase.name
    val aliasName    = aliasConfiguration.getName
    val aliasVersion = aliasConfiguration.getFunctionVersion
    val aliasArn     = aliasConfiguration.getAliasArn
    val publishedArn = lambda.generateLambdaArn(so.provider.awsAccount)(name)(Some(aliasVersion))
  }

  private case class FunctionAndListAliasesResult(
      private val functionBase: FunctionBase,
      private val listAliasesResult: ListAliasesResult) {
    val functionAndAliases =
      listAliasesResult.getAliases.asScala map { b =>
        FunctionAndAlias(functionBase, b)
      }
  }

  private def getHttpEventAliases: Try[Seq[FunctionAndAlias]] =
    sequence {
      so.functions.filteredHttpEvents map { func =>
        lambda
          .listAliases(func.name)
          .map(FunctionAndListAliasesResult(func, _))
      }
    }.map(_.flatMap(_.functionAndAliases))

  private def getStreamEventAliases: Try[Seq[FunctionAndAlias]] =
    sequence {
      so.functions.filteredStreamEvents map { func =>
        lambda
          .listAliases(func.name)
          .map(FunctionAndListAliasesResult(func, _))
      }
    }.map(_.flatMap(_.functionAndAliases))

  private def getStreamEventVersions: Try[Seq[FunctionConfiguration]] =
    sequence {
      so.functions.filteredStreamEvents map { func =>
        lambda.listVersionsByFunction(func.name)
      }
    }.map(_.flatMap(_.getVersions.asScala))

  private def skip[E](t: Try[E]): Try[Unit] =
    t match {
      case Success(_) => Try()
      case Failure(e) =>
        println(e.getMessage)
        Try()
    }

  private def deleteAliases(exportFunctionArns: Seq[String],
                            aliases: Seq[FunctionAndAlias]): Try[Unit] = {
    val diff              = aliases.map(_.aliasArn) diff exportFunctionArns
    val aliasMap          = aliases.map(a => a.aliasArn -> a).toMap
    val deletionCandidate = diff flatMap aliasMap.get
    for {
      _ <- sequence {
        deletionCandidate map { d =>
          skip {
            lambda.deleteAlias(d.name, d.aliasName)
          }
        }
      }
    } yield ()
  }

  private def deletePublishes(exportFunctionArns: Seq[String],
                              aliases: Seq[FunctionAndAlias]): Try[Unit] = {
    val aliasMap          = aliases.map(a => a.aliasArn -> a).toMap
    val notDeletion       = exportFunctionArns flatMap aliasMap.get
    val deletionCandidate = aliases diff notDeletion
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
                                deployments: GetDeploymentsResult): Try[Unit] = {
    val usedDeploymentIds = stages.getItem.asScala.map(_.getDeploymentId)
    val unUsedDeploymentIds = deployments.getItems.asScala
      .map(_.getId) diff usedDeploymentIds
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

  private def cleanApi(restApiId: RestApiId) =
    for {
      stages             <- api.getStages(restApiId)
      exportFunctionArns <- getFunctionArns(restApiId, stages)
      aliases            <- getHttpEventAliases
      _                  <- deleteAliases(exportFunctionArns, aliases)
      _                  <- deletePublishes(exportFunctionArns, aliases)
      deployments        <- api.getDeployments(restApiId)
      _                  <- deleteDeployments(restApiId, stages, deployments)
    } yield ()

  def deleteNoUseVersion(versions: Seq[FunctionConfiguration], aliases: Seq[FunctionAndAlias]) =
    Try {
      val deletionCandidate = versions.map(_.getFunctionArn) diff aliases.map(_.publishedArn)
      deletionCandidate map { arn =>
        skip {
          lambda.delete(arn)
        }
      }
    }

  private def cleanStream =
    for {
      aliases  <- getStreamEventAliases
      versions <- getStreamEventVersions
      _        <- deleteNoUseVersion(versions, aliases)
    } yield ()

  def invoke: Try[Unit] =
    swap {
      so.restApiId map { restApiId =>
        for {
          _ <- Try(println("clean api"))
          _ <- cleanApi(restApiId)
          _ <- Try(println("clean stream"))
          _ <- cleanStream
        } yield {}
      }
    }.map(_ => ())
}

case class Clean(so: serverless.ServerlessOption) extends CleanBase
