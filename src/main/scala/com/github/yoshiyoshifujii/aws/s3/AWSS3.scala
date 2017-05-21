package com.github.yoshiyoshifujii.aws.s3

import java.io.File

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest}
import com.github.yoshiyoshifujii.aws.{AWSCredentials, AWSWrapper}

import scala.util.Try

trait AWSS3Wrapper extends AWSWrapper {
  val regionName: String
  lazy val client = AmazonS3ClientBuilder
    .standard()
    .withCredentials(AWSCredentials.provider)
    .withRegion(regionName)
    .build()

  def putIfDoesNotObjectExist(bucketName: String, jar: File): Try[String] = {
    val key = jar.getName
    for {
      exist <- doesObjectExist(bucketName, key)
      res   <- if (exist) Try(key) else put(bucketName, key, jar)
    } yield res
  }

  private def doesObjectExist(bucketName: String, key: String) = Try {
    client.doesObjectExist(bucketName, key)
  }

  def put(bucketName: String, jar: File): Try[String] =
    put(bucketName, jar.getName, jar)

  private def put(bucketName: String, key: String, jar: File): Try[String] = Try {
    val objectRequest = new PutObjectRequest(bucketName, key, jar)
    objectRequest.setCannedAcl(CannedAccessControlList.AuthenticatedRead)

    client.putObject(objectRequest)

    key
  }
}

class AWSS3(val regionName: String) extends AWSS3Wrapper
