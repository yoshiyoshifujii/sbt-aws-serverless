package com.github.yoshiyoshifujii.aws.serverless.keys

import serverless.{Function => ServerlessFunction}
import scala.util.Try

trait DeployAlias extends KeysBase {
  val version: Option[String]

  private[keys] type PublishedVersion = Option[String]

  protected def generateLambdaAlias(prefix: String, publishedVersion: PublishedVersion) =
    publishedVersion.map(p => s"${prefix}_$p").getOrElse(prefix)

  protected def generateFunctionVersion(publishedVersion: PublishedVersion) =
    publishedVersion

  private def deployAlias(function: ServerlessFunction,
                          aliasName: String,
                          functionVersion: Option[String],
                          description: Option[String]) =
    for {
      aOp <- lambda.getAlias(function.name, aliasName)
      aliasArn <- aOp map { _ =>
        lambda.updateAlias(
          functionName = function.name,
          name = aliasName,
          functionVersion = functionVersion,
          description = description
        ).map(_.getAliasArn)
      } getOrElse {
        for {
          a <- lambda.createAlias(
            functionName = function.name,
            name = aliasName,
            functionVersion = functionVersion,
            description = description
          )
          _ <- lambda.addPermission(
            functionArn = a.getAliasArn
          )
        } yield a.getAliasArn
      }
      _ = { println(s"Lambda Alias: $aliasArn") }
    } yield ()

  protected def deployAlias(stage: String,
                            function: ServerlessFunction,
                            publishedVersion: PublishedVersion): Try[Unit] =
    Seq(
      function.events.ifHasHttpEventDo {
        () => deployAlias(
          function = function,
          aliasName = generateLambdaAlias(stage, publishedVersion),
          functionVersion = generateFunctionVersion(publishedVersion),
          description = version
        )
      },
      function.events.ifHasNotHttpEventDo {
        () => deployAlias(
          function = function,
          aliasName = stage,
          functionVersion = generateFunctionVersion(publishedVersion),
          description = version
        )
      }
    ).flatten
      .foldLeft(Try())(
        (c, d) => c.flatMap(_ => d())
      )
}
