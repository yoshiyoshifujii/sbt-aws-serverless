package com.github.yoshiyoshifujii

import java.io.{BufferedInputStream, File, FileInputStream}
import java.nio.ByteBuffer

import com.amazonaws.auth._

package object aws {

  object AWSCredentials {
    lazy val provider: AWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()
  }

  object IOUtils {

    def toByteArray(file: File) = {
      val bis = new BufferedInputStream(new FileInputStream(file))
      Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
    }
  }

  trait AWSWrapper {
    def toByteBuffer(file: File) = ByteBuffer.wrap(IOUtils.toByteArray(file))

    lazy val findAlias = (aliasArn: String) =>
      new scala.util.matching.Regex("""^.*:(.*)$""", "alias")
        .findAllIn(aliasArn)
        .matchData
        .map(_.group("alias"))
        .mkString("")
  }

  def ?[A](f: => A) =
    sbt.SimpleReader.readLine("delete ok? ") foreach { a =>
      if ("y" == a.trim.toLowerCase) f
    }

}
