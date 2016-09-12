package com.sample

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}

class Hello extends RequestStreamHandler {
  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val inputChars = Stream.continually(input.read).map(_.toChar).toArray
    println(String.valueOf(inputChars))
  }
}
