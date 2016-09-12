package com.github.yoshiyoshifujii.aws.s3

import java.io.File

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest}
import com.amazonaws.regions.RegionUtils
import com.github.yoshiyoshifujii.aws.AWSWrapper

import scala.util.Try

trait AWSS3Wrapper extends AWSWrapper {
  val regionName: String
  lazy val client = {
    val c = new AmazonS3Client()
    c.setRegion(RegionUtils.getRegion(regionName))
    c
  }

  def put(bucketName: String, jar: File) = Try {
    val key = jar.getName
    val objectRequest = new PutObjectRequest(bucketName, key, jar)
    objectRequest.setCannedAcl(CannedAccessControlList.AuthenticatedRead)

    client.putObject(objectRequest)

    key
  }
}

class AWSS3(val regionName: String) extends AWSS3Wrapper

