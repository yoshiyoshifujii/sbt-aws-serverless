package com.github.yoshiyoshifujii.aws.lambda

import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model._
import com.github.yoshiyoshifujii.aws.{AWSCredentials, AWSWrapper}
import com.github.yoshiyoshifujii.cliformatter.CliFormatter

import scala.collection.JavaConverters._
import scala.util.Try

trait AWSLambdaWrapper extends AWSWrapper {

  val regionName: String

  lazy val client = AWSLambdaClientBuilder
    .standard()
    .withCredentials(AWSCredentials.provider)
    .withRegion(regionName)
    .build()

  private def toOpt[A](f: => A) =
    try {
      Some(f)
    } catch {
      case _: ResourceNotFoundException => None
    }

  lazy val generateLambdaArn =
    (awsAccount: String) =>
      (lambdaName: String) => s"arn:aws:lambda:$regionName:$awsAccount:function:$lambdaName"

  def create(functionName: FunctionName,
             role: Role,
             handler: Handler,
             bucketName: String,
             key: String,
             description: Option[Description],
             timeout: Option[Timeout],
             memorySize: Option[MemorySize],
             environment: Option[Map[String, String]],
             reservedConcurrentExecutions: Option[Int],
             tags: Option[Map[String, String]],
             tracingMode: Option[TracingMode]) = {
    val code = new FunctionCode()
      .withS3Bucket(bucketName)
      .withS3Key(key)
    for {
      cf <- Try {
        val request = new CreateFunctionRequest()
          .withFunctionName(functionName)
          .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8Al2)
          .withRole(role)
          .withHandler(handler)
          .withCode(code)
        description.foreach(request.setDescription)
        timeout.foreach(request.setTimeout(_))
        memorySize.foreach(request.setMemorySize(_))
        environment.foreach(e => request.setEnvironment(new Environment().withVariables(e.asJava)))
        tags.foreach(t => request.setTags(t.asJava))
        tracingMode.foreach(m => request.setTracingConfig(new TracingConfig().withMode(m)))

        val createdFunction = client.createFunction(request)

        reservedConcurrentExecutions match {
          case Some(execs) =>
            client.putFunctionConcurrency(
              new PutFunctionConcurrencyRequest()
                .withFunctionName(functionName)
                .withReservedConcurrentExecutions(execs)
            )
          case None =>
            client.deleteFunctionConcurrency(
              new DeleteFunctionConcurrencyRequest()
                .withFunctionName(functionName)
            )
        }

        createdFunction
      }
    } yield cf
  }

  def get(functionName: FunctionName) = Try {
    val request = new GetFunctionRequest()
      .withFunctionName(functionName)

    toOpt(client.getFunction(request))
  }

  def update(functionName: FunctionName, bucketName: String, key: String) = {
    for {
      uf <- Try {
        val request = new UpdateFunctionCodeRequest()
          .withFunctionName(functionName)
          .withS3Bucket(bucketName)
          .withS3Key(key)

        client.updateFunctionCode(request)
      }
    } yield uf
  }

  def updateConfig(functionName: FunctionName,
                   role: Role,
                   handler: Handler,
                   description: Option[Description],
                   timeout: Option[Timeout],
                   memorySize: Option[MemorySize],
                   environment: Option[Map[String, String]],
                   reservedConcurrentExecutions: Option[Int],
                   tracingMode: Option[TracingMode]) = Try {
    val request = new UpdateFunctionConfigurationRequest()
      .withFunctionName(functionName)
      .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8Al2)
      .withHandler(handler)
      .withRole(role)
    description.foreach(request.setDescription)
    timeout.foreach(request.setTimeout(_))
    memorySize.foreach(request.setMemorySize(_))
    environment.foreach(e => request.setEnvironment(new Environment().withVariables(e.asJava)))
    tracingMode.foreach(m => request.setTracingConfig(new TracingConfig().withMode(m)))

    val updatedFunction = client.updateFunctionConfiguration(request)

    reservedConcurrentExecutions match {
      case Some(execs) =>
        client.putFunctionConcurrency(
          new PutFunctionConcurrencyRequest()
            .withFunctionName(functionName)
            .withReservedConcurrentExecutions(execs)
        )
      case None =>
        client.deleteFunctionConcurrency(
          new DeleteFunctionConcurrencyRequest()
            .withFunctionName(functionName)
        )
    }

    updatedFunction
  }

  def tagResource(functionArn: FunctionArn, tags: Map[String, String]) = Try {
    val request = new TagResourceRequest()
      .withResource(functionArn)
      .withTags(tags.asJava)

    client.tagResource(request)
  }

  def delete(functionName: FunctionName) = Try {
    val request = new DeleteFunctionRequest()
      .withFunctionName(functionName)

    client.deleteFunction(request)
  }

  def addPermission(functionArn: FunctionArn) = Try {
    val request = new AddPermissionRequest()
      .withFunctionName(functionArn)
      .withStatementId("apigateway-invoke-lambda")
      .withAction("lambda:InvokeFunction")
      .withPrincipal("apigateway.amazonaws.com")

    client.addPermission(request)
  }

  def publishVersion(functionName: FunctionName, description: Option[Description]) = Try {
    val request = new PublishVersionRequest()
      .withFunctionName(functionName)
    description.foreach(request.setDescription)

    client.publishVersion(request)
  }

  def listVersionsByFunction(functionName: FunctionName) = Try {
    val request = new ListVersionsByFunctionRequest()
      .withFunctionName(functionName)

    client.listVersionsByFunction(request)
  }

  def printListVersionsByFunction(functionName: FunctionName) =
    for {
      l <- listVersionsByFunction(functionName)
    } yield {
      val p = CliFormatter(
        functionName,
        "Last modified" -> 30,
        "Ver"           -> 12,
        "Description"   -> 45
      ).print3(l.getVersions.asScala.map { v =>
        (v.getLastModified, v.getVersion, v.getDescription)
      }: _*)
      println(p)
    }

  def deploy(functionName: FunctionName,
             role: Role,
             handler: Handler,
             bucketName: String,
             key: String,
             description: Option[Description],
             timeout: Option[Timeout],
             memorySize: Option[MemorySize],
             environment: Option[Map[String, String]],
             reservedConcurrentExecutions: Option[Int],
             tags: Option[Map[String, String]],
             tracingMode: Option[TracingMode],
             createAfter: FunctionArn => Try[Any] = _ => Try(())) = {
    for {
      gfr <- get(functionName)
      arn <- gfr map { _ =>
        for {
          _ <- update(functionName, bucketName, key)
          uc <- updateConfig(
            functionName = functionName,
            role = role,
            handler = handler,
            description = description,
            timeout = timeout,
            memorySize = memorySize,
            environment = environment,
            reservedConcurrentExecutions = reservedConcurrentExecutions,
            tracingMode = tracingMode
          )
          _ <- tags map { t =>
            tagResource(uc.getFunctionArn, t)
          } getOrElse Try(())
        } yield uc.getFunctionArn
      } getOrElse {
        for {
          c <- create(
            functionName = functionName,
            role = role,
            handler = handler,
            bucketName = bucketName,
            key = key,
            description = description,
            timeout = timeout,
            memorySize = memorySize,
            environment = environment,
            reservedConcurrentExecutions = reservedConcurrentExecutions,
            tags = tags,
            tracingMode = tracingMode
          )
          _ <- createAfter(c.getFunctionArn)
        } yield c.getFunctionArn
      }
    } yield arn
  }

  def listEventSourceMappings(functionArn: FunctionArn) = Try {
    val request = new ListEventSourceMappingsRequest()
      .withFunctionName(functionArn)

    client.listEventSourceMappings(request)
  }

  def printEventSourceMappings(functionArn: FunctionArn) =
    for {
      l <- listEventSourceMappings(functionArn)
    } yield {
      val p = CliFormatter(
        functionArn,
        "Last modified"    -> 30,
        "State"            -> 12,
        "UUID"             -> 40,
        "Event Source Arn" -> 100
      ).print4(l.getEventSourceMappings.asScala.map { e =>
        (e.getLastModified.toString, e.getState, e.getUUID, e.getEventSourceArn)
      }: _*)
      println(p)
    }

  def createEventSourceMapping(
      functionArn: FunctionArn,
      eventSourceArn: EventSourceArn,
      enabled: Boolean = true,
      batchSize: Int = 100,
      startPosition: EventSourcePosition = EventSourcePosition.TRIM_HORIZON) = Try {
    val request = new CreateEventSourceMappingRequest()
      .withEventSourceArn(eventSourceArn)
      .withFunctionName(functionArn)
      .withEnabled(enabled)
      .withBatchSize(batchSize)
      .withStartingPosition(startPosition)

    client.createEventSourceMapping(request)
  }

  def deleteEventSourceMapping(uuid: String) = Try {
    val request = new DeleteEventSourceMappingRequest()
      .withUUID(uuid)

    client.deleteEventSourceMapping(request)
  }

  def deleteEventSourceMappings(functionArn: FunctionArn) =
    for {
      l <- listEventSourceMappings(functionArn)
      d <- Try(l.getEventSourceMappings.asScala.map(e => deleteEventSourceMapping(e.getUUID).get))
    } yield d
}

case class AWSLambda(regionName: String) extends AWSLambdaWrapper
