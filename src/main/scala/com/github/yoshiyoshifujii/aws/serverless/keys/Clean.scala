package com.github.yoshiyoshifujii.aws.serverless.keys

import java.nio.charset.StandardCharsets

import com.amazonaws.services.apigateway.model.{
  GetDeploymentsResult,
  GetExportResult,
  GetStagesResult,
  Stage
}
import com.amazonaws.services.lambda.model.{AliasConfiguration, ListAliasesResult}
import com.github.yoshiyoshifujii.aws.apigateway.RestApiId
import serverless.FunctionBase

import scala.collection.JavaConverters._
import scala.util.Try

trait CleanBase extends KeysBase {

  private def getFunctionArn(stage: Stage, export: GetExportResult): Iterator[String] = {
    val json   = new String(export.getBody.array, StandardCharsets.UTF_8)
    val envOpt = stage.getVariables.asScala.get("env")
    """"uri" : ".*/functions/(.*)/invocations"""".r.findAllMatchIn(json) map { m =>
      envOpt map { env =>
        m.group(1).replaceAll("\\$\\{stageVariables.env\\}", env)
      } getOrElse {
        m.group(1)
      }
    }
  }

  private def getFunctionArns(restApiId: RestApiId, stages: GetStagesResult): Try[Seq[String]] =
    sequence {
      stages.getItem.asScala map { stage =>
        api.export(restApiId, stage.getStageName) map { export =>
          getFunctionArn(stage, export)
        }
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
        lambda.listAliases(func.name).map(FunctionAndListAliasesResult(func, _))
      }
    }.map(_.flatMap(_.functionAndAliases))

  private def deleteAliases(exportFunctionArns: Seq[String],
                            aliases: Seq[FunctionAndAlias]): Try[Unit] = {
    val diff              = aliases.map(_.aliasArn) diff exportFunctionArns
    val aliasMap          = aliases.map(a => a.aliasArn -> a).toMap
    val deletionCandidate = diff flatMap aliasMap.get
    for {
      _ <- sequence {
        deletionCandidate map { d =>
//          lambda.deleteAlias(d.name, d.aliasName)
          println(d.name, d.aliasName)
          Try()
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
//            lambda.delete(arn)
            println(arn)
            Try()
          }
      }
    } yield ()
  }

  private def deleteDeployments(restApiId: RestApiId,
                                stages: GetStagesResult,
                                deployments: GetDeploymentsResult): Try[Unit] = {
    val usedDeploymentIds   = stages.getItem.asScala.map(_.getDeploymentId)
    val unUsedDeploymentIds = deployments.getItems.asScala.map(_.getId) diff usedDeploymentIds
    for {
      _ <- sequence {
        unUsedDeploymentIds map { id =>
//          api.deleteDeployment(restApiId, id)
          println(restApiId, id)
          Try()
        }
      }
    } yield ()
  }

  def invoke: Try[Unit] =
    swap {
      so.restApiId map { restApiId =>
        for {
          stages             <- api.getStages(restApiId)
          exportFunctionArns <- getFunctionArns(restApiId, stages)
          aliases            <- getHttpEventAliases
          _                  <- deleteAliases(exportFunctionArns, aliases)
          _                  <- deletePublishes(exportFunctionArns, aliases)
          deployments        <- api.getDeployments(restApiId)
          _                  <- deleteDeployments(restApiId, stages, deployments)
        } yield {}
      }
    }.map(_ => ())
}

case class Clean(so: serverless.ServerlessOption) extends CleanBase
