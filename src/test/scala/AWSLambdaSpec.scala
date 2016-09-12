import com.github.yoshiyoshifujii.aws.lambda.AWSLambdaWrapper
import com.typesafe.config.ConfigFactory
import org.scalatest._
import sbt.File

import scala.util.{Failure, Success}

class AWSLambdaSpec extends FlatSpec with Matchers {

  "AWSLambdaSpec" should "success" in new WithFixture {
    val functionName = "sampleScalaLambda"

    val jar = new File("sample/lambda/target/scala-2.10/sampleScalaLambda-assembly-0.1-SNAPSHOT.jar")
    jar.exists() === true

    get(
      functionName = functionName
    ) match {
      case Success(s) => s match {
        case Some(s2) => delete(s2.getConfiguration.getFunctionName)
        case None =>
      }
      case Failure(e) => fail(e)
    }

    create(
      functionName = functionName,
      description = Some("sample"),
      handler = "com.sample.Hello::handleRequest",
      role = s"arn:aws:iam::$AwsAccountId:role/$AwsRoleName",
      bucketName = AwsBucketName,
      jar = jar,
      timeout = Some(15),
      memorySize = Some(1024)
    ) match {
      case Success(s) =>
        s.getFunctionArn === s"arn:aws:lambda:us-east-1:$AwsAccountId:function:sampleScalaLambda"
        println(s"create success: ${s.getFunctionArn}")
      case Failure(e) => fail(e)
    }

    get(
      functionName = functionName
    ) match {
      case Success(s) => s match {
        case Some(s2) =>
          println(s"get success: ${s2.getConfiguration.getFunctionArn}")
          (for {
            u <- update(s2.getConfiguration.getFunctionName, AwsBucketName, jar)
            u2 <- updateConfig(
              functionName = functionName,
              description = Some("hoge"),
              handler = "com.sample.Hello::handleRequest",
              role = s"arn:aws:iam::$AwsAccountId:role/$AwsRoleName",
              timeout = Some(15),
              memorySize = Some(1024))
          } yield {
            u2.getFunctionArn === s"arn:aws:lambda:us-east-1:$AwsAccountId:function:sampleScalaLambda"
            println(s"update success: ${u2.getFunctionArn}")
          }).isSuccess === true
          delete(functionName).isSuccess === true
          println("delete success")
        case None => fail
      }
      case Failure(e) => fail(e)
    }
  }

  "AWSLambdaSpec" should "success deploy" in new WithFixture {
    val functionName = "sampleScalaLambda2"

    val jar = new File("sample/lambda/target/scala-2.10/sampleScalaLambda-assembly-0.1-SNAPSHOT.jar")
    jar.exists() === true

    deploy(
      functionName = functionName,
      handler = "com.sample.Hello::handleRequest",
      role = s"arn:aws:iam::$AwsAccountId:role/$AwsRoleName",
      bucketName = AwsBucketName,
      jar = jar,
      description = Some("sample"),
      timeout = Some(15),
      memorySize = Some(1024)) === jar

    get(functionName) match {
      case Success(s) =>
        s.isDefined === true
        val conf = s.get.getConfiguration
        conf.getDescription === "sample"
        conf.getHandler === "com.sample.Hello::handleRequest"
        conf.getRole === s"arn:aws:iam::$AwsAccountId:role/$AwsRoleName"
        conf.getTimeout === 15
        conf.getMemorySize === 1024
        delete(functionName).isSuccess === true
      case Failure(e) => fail(e)
    }
  }

  trait WithFixture extends AWSLambdaWrapper {
    override val regionName = "us-east-1"
    lazy val config = ConfigFactory.load
    lazy val AwsAccountId = config.getString("aws.account.id")
    lazy val AwsRoleName = config.getString("aws.role.name")
    lazy val AwsBucketName = config.getString("aws.bucket.name")
  }
}
