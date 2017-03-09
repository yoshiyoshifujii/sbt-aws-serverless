import sbt._

package object serverless {

  case class Provider(awsAccount: String,
                      region: String = "us-east-1",
                      deploymentBucket: String)

  case class ApiGateway(swagger: File,
                        restApiId: Option[String] = None,
                        stageVariables: Option[Map[String, String]] = None) {
    lazy val getStageVariables: (String, String) => Option[Map[String, String]] =
      (region, stage) =>
        stageVariables.orElse(Some(Map(
          "region" -> region,
          "env" -> stage
        )))
  }

  sealed trait FunctionBase {
    val name: String
    val events: Events
  }

  case class Function(filePath: File,
                      name: String,
                      description: Option[String] = None,
                      handler: String,
                      memorySize: Int = 512,
                      timeout: Int = 10,
                      role: String,
                      environment: Map[String, String] = Map.empty,
                      events: Events = Events.empty) extends FunctionBase {

    lazy val getEnvironment: String => Map[String, String] =
      (stage: String) =>
        if (environment.isEmpty)
          Map("stage" -> stage)
        else
          environment
  }

  case class NotDeployLambdaFunction(name: String,
                                     publishedVersion: Option[String] = None,
                                     events: Events = Events.empty) extends FunctionBase

  case class Functions(private val functions: FunctionBase*) {

    private lazy val sortedFunctions = functions.sortWith {
      (a, b) => {
        a.events.hasAuthorizeEvent.compareTo(b.events.hasAuthorizeEvent) > 0
      }
    }

    def map[B](f: FunctionBase => B) = sortedFunctions.map(f)

    def notExistsFilePathFunctions = for {
      fb <- functions
      _ <- fb match {
        case a: Function if a.filePath.exists() => Some(a)
        case _ => None
      }
    } yield fb

    def find(functionName: String) = functions.find(f => f.name == functionName)

    lazy val filteredHttpEvents = functions.filter(_.events.hasHttpEvent)

    lazy val filteredStreamEvents = functions.filter(_.events.hasStreamEvent)
  }

  case class ServerlessOption(provider: Provider,
                              apiGateway: Option[ApiGateway],
                              functions: Functions)

  object ServerlessOption {

    def apply(provider: Provider,
              functions: Functions): ServerlessOption =
      new ServerlessOption(provider, None, functions)

    def apply(provider: Provider,
              apiGateway: ApiGateway,
              functions: Functions): ServerlessOption =
      new ServerlessOption(provider, Some(apiGateway), functions)
  }

}
