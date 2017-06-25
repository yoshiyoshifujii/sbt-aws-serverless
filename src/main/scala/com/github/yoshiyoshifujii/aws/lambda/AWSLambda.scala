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
      (lambdaName: String) =>
        (lambdaAlias: Option[String]) =>
          lambdaAlias map { alias =>
            s"arn:aws:lambda:$regionName:$awsAccount:function:$lambdaName:$alias"
          } getOrElse {
            s"arn:aws:lambda:$regionName:$awsAccount:function:$lambdaName"
    }

  def create(functionName: FunctionName,
             role: Role,
             handler: Handler,
             bucketName: String,
             key: String,
             description: Option[Description],
             timeout: Option[Timeout],
             memorySize: Option[MemorySize],
             environment: Option[Map[String, String]],
             tracingMode: Option[TracingMode]) = {
    val code = new FunctionCode()
      .withS3Bucket(bucketName)
      .withS3Key(key)
    for {
      cf <- Try {
        val request = new CreateFunctionRequest()
          .withFunctionName(functionName)
          .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
          .withRole(role)
          .withHandler(handler)
          .withCode(code)
        description.foreach(request.setDescription)
        timeout.foreach(request.setTimeout(_))
        memorySize.foreach(request.setMemorySize(_))
        environment.foreach(e => request.setEnvironment(new Environment().withVariables(e.asJava)))
        tracingMode.foreach(m => request.setTracingConfig(new TracingConfig().withMode(m)))

        client.createFunction(request)
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
                   tracingMode: Option[TracingMode]) = Try {
    val request = new UpdateFunctionConfigurationRequest()
      .withFunctionName(functionName)
      .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
      .withHandler(handler)
      .withRole(role)
    description.foreach(request.setDescription)
    timeout.foreach(request.setTimeout(_))
    memorySize.foreach(request.setMemorySize(_))
    environment.foreach(e => request.setEnvironment(new Environment().withVariables(e.asJava)))
    tracingMode.foreach(m => request.setTracingConfig(new TracingConfig().withMode(m)))

    client.updateFunctionConfiguration(request)
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

  def createAlias(functionName: FunctionName,
                  name: String,
                  functionVersion: Option[String],
                  description: Option[Description]) = Try {
    val request = new CreateAliasRequest()
      .withFunctionName(functionName)
      .withName(name)
      .withFunctionVersion(functionVersion.getOrElse("$LATEST"))
    description.foreach(request.setDescription)

    client.createAlias(request)
  }

  def updateAlias(functionName: FunctionName,
                  name: String,
                  functionVersion: Option[String],
                  description: Option[Description]) = Try {
    val request = new UpdateAliasRequest()
      .withFunctionName(functionName)
      .withName(name)
      .withFunctionVersion(functionVersion.getOrElse("$LATEST"))
    description.foreach(request.setDescription)

    client.updateAlias(request)
  }

  def getAlias(functionName: FunctionName, name: String) = Try {
    val request = new GetAliasRequest()
      .withFunctionName(functionName)
      .withName(name)

    toOpt(client.getAlias(request))
  }

  def deleteAlias(functionName: FunctionName, name: String) = Try {
    val request = new DeleteAliasRequest()
      .withFunctionName(functionName)
      .withName(name)

    client.deleteAlias(request)
  }

  def createOrUpdateAlias(functionName: FunctionName,
                          name: String,
                          functionVersion: Option[String],
                          description: Option[Description]) =
    for {
      aliasOpt <- getAlias(functionName, name)
      aliasArn <- aliasOpt map { _ =>
        updateAlias(functionName, name, functionVersion, description).map(_.getAliasArn)
      } getOrElse {
        createAlias(functionName, name, functionVersion, description).map(_.getAliasArn)
      }
    } yield aliasArn

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

  def listAliases(functionName: FunctionName) = Try {
    val request = new ListAliasesRequest()
      .withFunctionName(functionName)

    client.listAliases(request)
  }

  def printListAliases(functionName: FunctionName) =
    for {
      l <- listAliases(functionName)
    } yield {
      val p = CliFormatter(
        functionName,
        "Alias name"  -> 20,
        "Ver"         -> 12,
        "Description" -> 45
      ).print3(l.getAliases.asScala.map { a =>
        (findAlias(a.getAliasArn), a.getFunctionVersion, a.getDescription)
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
             tracingMode: Option[TracingMode],
             createAfter: FunctionArn => Try[Any] = _ => Try()) = {
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
            tracingMode = tracingMode
          )
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

  def createEventSourceMapping(functionArn: FunctionArn,
                               eventSourceArn: EventSourceArn,
                               enabled: Boolean = true,
                               batchSize: Int = 100,
                               startPosition: EventSourcePosition =
                                 EventSourcePosition.TRIM_HORIZON) = Try {
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
