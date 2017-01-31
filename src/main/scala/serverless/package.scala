import sbt._

package object serverless {

  case class Provider(awsAccount: String,
                      stage: String = "dev",
                      region: String = "us-east-1",
                      deploymentBucket: String,
                      swagger: File)

  case class Function(filePath: File,
                      name: String,
                      description: Option[String] = None,
                      handler: String,
                      memorySize: Int = 512,
                      timeout: Int = 10,
                      role: String,
                      environment: Map[String, String] = Map.empty,
                      events: Events)

  case class Functions(private val functions: Function*) {

    def foreach[U](f: Function => U): Unit = functions.foreach(f)

  }

  case class ServerlessOption(provider: Provider,
                              functions: Functions)

}
