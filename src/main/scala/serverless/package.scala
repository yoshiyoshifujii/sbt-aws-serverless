import com.amazonaws.services.lambda.model.TracingMode
import com.github.yoshiyoshifujii.aws.apigateway.RestApiId
import sbt._

import scala.util.Try

package object serverless {

  case class Provider(awsAccount: String, region: String = "us-east-1", deploymentBucket: String)

  case class ApiGateway(swagger: File, stageVariables: Option[Map[String, String]] = None) {
    private val restApiIdFileName = ".sbt-serverless"
    private val restApiIdFile     = file(restApiIdFileName)

    def restApiId: Option[RestApiId] =
      if (restApiIdFile.exists()) Some(IO.read(restApiIdFile, IO.utf8).trim) else None

    def writeRestApiId(restApiId: RestApiId): Try[Unit] = Try {
      IO.write(restApiIdFile, restApiId, IO.utf8)
    }

    def deleteRestApiId(): Try[Unit] = Try {
      IO.delete(restApiIdFile)
    }

    lazy val getStageVariables: (String, String) => Option[Map[String, String]] =
      (region, stage) =>
        stageVariables.orElse(
          Some(
            Map(
              "region" -> region,
              "env"    -> stage
            )))
  }

  sealed trait FunctionBase {
    protected val name: String
    val events: Events

    def nameWith(stage: String): String           = s"$name-$stage"
    def equalsName(functionName: String): Boolean = name == functionName
  }

  case class Function(filePath: File,
                      protected val name: String,
                      description: Option[String] = None,
                      handler: String,
                      memorySize: Int = 512,
                      timeout: Int = 10,
                      role: String,
                      environment: Map[String, String] = Map.empty,
                      reservedConcurrentExecutions: Option[Int] = None,
                      tags: Map[String, String] = Map.empty,
                      tracing: Option[Tracing] = None,
                      events: Events = Events.empty)
      extends FunctionBase {

    def getEnvironment(stage: String): Map[String, String] =
      if (environment.isEmpty) Map("stage" -> stage) else environment + ("stage" -> stage)
  }

  case class NotDeployLambdaFunction(protected val name: String,
                                     publishedVersion: Option[String] = None,
                                     events: Events = Events.empty)
      extends FunctionBase

  case class Functions(private val functions: FunctionBase*) {

    private lazy val sortedFunctions = functions.sortWith { (a, b) =>
      a.events.hasAuthorizeEvent.compareTo(b.events.hasAuthorizeEvent) > 0
    }

    def notExistsFilePathFunctions =
      for {
        fb <- functions
        _ <- fb match {
          case a: Function if !a.filePath.exists() => Some(a)
          case _                                   => None
        }
      } yield fb

    def map[B](f: FunctionBase => B) = sortedFunctions.map(f)
    def find(functionName: String)   = functions.find(_.equalsName(functionName))
    lazy val filteredHttpEvents      = functions.filter(_.events.hasHttpEvent)
    lazy val filteredStreamEvents    = functions.filter(_.events.hasStreamEvent)
  }

  case class ServerlessOption(provider: Provider,
                              apiGateway: Option[ApiGateway],
                              functions: Functions) {
    lazy val restApiId = apiGateway.flatMap(_.restApiId)
  }

  sealed abstract class Tracing(val value: TracingMode)

  object Tracing {
    case object PassThrough extends Tracing(TracingMode.PassThrough)
    case object Active      extends Tracing(TracingMode.Active)
  }

  object ServerlessOption {

    def apply(provider: Provider, functions: Functions): ServerlessOption =
      new ServerlessOption(provider, None, functions)

    def apply(provider: Provider, apiGateway: ApiGateway, functions: Functions): ServerlessOption =
      new ServerlessOption(provider, Some(apiGateway), functions)
  }

}
