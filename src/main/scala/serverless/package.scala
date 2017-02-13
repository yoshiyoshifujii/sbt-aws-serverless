import sbt._

package object serverless {

  case class Provider(awsAccount: String,
                      stage: String = "dev",
                      region: String = "us-east-1",
                      deploymentBucket: String,
                      swagger: File,
                      restApiId: Option[String] = None,
                      stageVariables: Option[Map[String, String]] = None) {

    lazy val getStageVariables: Option[Map[String, String]] =
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

    def foreach[U](f: Function => U): Unit = functions.foreach(f)

    def map[B](f: Function => B) = functions.map(f)

    def find(functionName: String) = functions.find(f => f.name == functionName)

  }

  case class ServerlessOption(provider: Provider,
                              functions: Functions)

}
