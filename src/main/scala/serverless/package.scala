import sbt._

package object serverless {

  case class Provider(awsAccount: String,
                      region: String = "us-east-1",
                      deploymentBucket: String,
                      swagger: File,
                      restApiId: Option[String] = None,
                      stageVariables: Option[Map[String, String]] = None) {

    lazy val getStageVariables: String => Option[Map[String, String]] =
      (stage: String) =>
        stageVariables.orElse(Some(Map(
          "env" -> stage,
          "region" -> region
        )))
  }

  case class Function(filePath: File,
                      name: String,
                      description: Option[String] = None,
                      handler: String,
                      memorySize: Int = 512,
                      timeout: Int = 10,
                      role: String,
                      environment: Map[String, String] = Map.empty,
                      events: Events = Events.empty)

  case class Functions(private val functions: Function*) {

    private lazy val sortedFunctions = functions.sortWith {
      (a, b) => {
        a.events.hasAuthorizeEvent.compareTo(b.events.hasAuthorizeEvent) > 0
      }
    }

    def map[B](f: Function => B) = sortedFunctions.map(f)

    def find(functionName: String) = functions.find(f => f.name == functionName)

    lazy val filteredHttpEvents = functions.filter(_.events.hasHttpEvents)

  }

  case class ServerlessOption(provider: Provider,
                              functions: Functions)

}
