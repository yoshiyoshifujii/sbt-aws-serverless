package com.github.yoshiyoshifujii.aws.lambda

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model._
import com.github.yoshiyoshifujii.aws.s3.AWSS3
import com.github.yoshiyoshifujii.aws.{AWSCredentials, AWSWrapper}
import com.github.yoshiyoshifujii.cliformatter.CliFormatter
import sbt._

import scala.collection.JavaConversions._
import scala.util.Try

trait AWSLambdaWrapper extends AWSWrapper {

  val regionName: String

  lazy val s3 = new AWSS3(regionName)

  lazy val client = {
    val c = new AWSLambdaClient(AWSCredentials.provider)
    c.setRegion(RegionUtils.getRegion(regionName))
    c
  }

  private def toOpt[A](f: => A) =
    try {
      Some(f)
    } catch {
      case e: ResourceNotFoundException => None
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
             jar: File,
             description: Option[Description],
             timeout: Option[Timeout],
             memorySize: Option[MemorySize]) = {
    for {
      key <- s3.put(bucketName, jar)
      code = new FunctionCode()
        .withS3Bucket(bucketName)
        .withS3Key(key)
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

        client.createFunction(request)
      }
    } yield cf
  }

  def get(functionName: FunctionName) = Try {
    val request = new GetFunctionRequest()
      .withFunctionName(functionName)

    toOpt(client.getFunction(request))
  }

  def update(functionName: FunctionName,
             bucketName: String,
             jar: File) = {
    for {
      key <- s3.put(bucketName, jar)
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
                   memorySize: Option[MemorySize]) = Try {
    val request = new UpdateFunctionConfigurationRequest()
      .withFunctionName(functionName)
      .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
      .withHandler(handler)
      .withRole(role)
    description.foreach(request.setDescription)
    timeout.foreach(request.setTimeout(_))
    memorySize.foreach(request.setMemorySize(_))

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

  def publishVersion(functionName: FunctionName,
                     description: Option[Description]) = Try {
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

  def getAlias(functionName: FunctionName,
               name: String) = Try {
    val request = new GetAliasRequest()
      .withFunctionName(functionName)
      .withName(name)

    toOpt(client.getAlias(request))
  }

  def createOrUpdateAlias(functionName: FunctionName,
                          name: String,
                          functionVersion: Option[String],
                          description: Option[Description]) =
    for {
      aliasOpt <- getAlias(functionName, name)
      aliasArn <- aliasOpt map { a =>
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
        "Ver" -> 12,
        "Description" -> 45
      ).print3(
        l.getVersions.map { v =>
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
        "Alias name" -> 20,
        "Ver" -> 12,
        "Description" -> 45
      ).print3(
        l.getAliases.map { a =>
          (findAlias(a.getAliasArn), a.getFunctionVersion, a.getDescription)
        }: _*)
      println(p)
    }

  def deploy(functionName: FunctionName,
             role: Role,
             handler: Handler,
             bucketName: String,
             jar: File,
             description: Option[Description],
             timeout: Option[Timeout],
             memorySize: Option[MemorySize],
             createAfter: FunctionArn => Try[Any] = arn => Try()) = {
    for {
      gfr <- get(functionName)
      arn <- gfr map { f =>
        for {
          u <- update(functionName, bucketName, jar)
          uc <- updateConfig(
            functionName = functionName,
            role = role,
            handler = handler,
            description = description,
            timeout = timeout,
            memorySize = memorySize)
        } yield uc.getFunctionArn
      } getOrElse {
        for {
          c <- create(
            functionName = functionName,
            role = role,
            handler = handler,
            bucketName = bucketName,
            jar = jar,
            description = description,
            timeout = timeout,
            memorySize = memorySize)
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
        "Last modified" -> 30,
        "State" -> 12,
        "UUID" -> 40,
        "Event Source Arn" -> 100
      ).print4(
        l.getEventSourceMappings.map { e =>
          (e.getLastModified.toString, e.getState, e.getUUID, e.getEventSourceArn)
        }: _*)
      println(p)
    }

  def createEventSourceMapping(functionArn: FunctionArn,
                               eventSourceArn: EventSourceArn) = Try {
    val request = new CreateEventSourceMappingRequest()
      .withFunctionName(functionArn)
      .withEventSourceArn(eventSourceArn)
      .withBatchSize(100)
      .withStartingPosition(EventSourcePosition.TRIM_HORIZON)

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
      d <- Try(l.getEventSourceMappings.map(e => deleteEventSourceMapping(e.getUUID).get))
    } yield d
}

case class AWSLambda(regionName: String) extends AWSLambdaWrapper

