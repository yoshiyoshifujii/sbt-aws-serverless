package com.github.yoshiyoshifujii.aws.serverless

import com.github.yoshiyoshifujii.aws.apigateway.{AWSApiGatewayAuthorize, AWSApiGatewayMethods}
import com.github.yoshiyoshifujii.aws.lambda.{AWSLambda, FunctionName}

import scala.util.{Success, Try}

package object keys {

  private[keys] def deployAlias(lambda: AWSLambda,
                                 functionName: FunctionName,
                                 aliasName: String,
                                 functionVersion: Option[String],
                                 description: Option[String]) =
    for {
      aOp <- lambda.getAlias(functionName, aliasName)
      res <- aOp map { _ =>
        lambda.updateAlias(
          functionName = functionName,
          name = aliasName,
          functionVersion = functionVersion,
          description = description
        ).map(_.getAliasArn)
      } getOrElse {
        for {
          a <- lambda.createAlias(
            functionName = functionName,
            name = aliasName,
            functionVersion = functionVersion,
            description = description
          )
          _ <- lambda.addPermission(
            functionArn = a.getAliasArn
          )
        } yield a.getAliasArn
      }
    } yield res

  private[keys] lazy val withAuth =
    (method: AWSApiGatewayMethods) =>
      (authorize: AWSApiGatewayAuthorize) =>
        (authName: Option[String]) =>
          (resourceId: String) => Try {
            (authName map { name =>
              for {
                aOp <- authorize.getAuthorizer(name)
                r <- Try {
                  aOp map { a =>
                    method.updateMethod(
                      resourceId = resourceId,
                      "/authorizationType" -> "CUSTOM",
                      "/authorizerId" -> a.getId
                    ).get
                  } getOrElse(throw new RuntimeException(s"Custome Authorizer is nothing. $name"))
                }
              } yield r
            } getOrElse {
              method.updateMethod(
                resourceId = resourceId,
                "/authorizationType" -> "NONE"
              )
            }).get
            ()
          }

  private[keys] def sequence[T](xs : Seq[Try[T]]) : Try[Seq[T]] = (Try(Seq[T]()) /: xs) {
    (a, b) => a flatMap (c => b map (d => c :+ d))
  }

  private[keys] def swap[T](optTry: Option[Try[T]]): Try[Option[T]] =
    optTry.map(_.map(Some.apply)).getOrElse(Success(None))

}
