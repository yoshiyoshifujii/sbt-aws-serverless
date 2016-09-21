package com.github.yoshiyoshifujii.aws

import org.apache.http.client.methods._
import org.apache.http.entity.{ByteArrayEntity, ContentType}
import org.apache.http.impl.client.HttpClientBuilder

import scala.util.Try

package object http {

  private lazy val withHeaders = (requestBuilder: RequestBuilder) => (headers: Seq[(String, String)]) =>
    (requestBuilder /: headers)((b, h) => b.addHeader(h._1, h._2))

  private lazy val withParameters = (requestBuilder: RequestBuilder) => (parameters: Seq[(String, String)]) =>
    (requestBuilder /: parameters)((b, h) => b.addParameter(h._1, h._2))

  private lazy val withEntity = (requestBuilder: RequestBuilder) => (body: Option[Array[Byte]]) =>
    body map { b =>
      requestBuilder.setEntity(new ByteArrayEntity(b, ContentType.APPLICATION_JSON))
    } getOrElse requestBuilder

  private lazy val doRequest =
    (requestBuilder: RequestBuilder) =>
      (headers: Seq[(String, String)]) =>
        (parameters: Seq[(String, String)]) =>
          (body: Option[Array[Byte]]) => Try {
            val request =
              withHeaders {
                withParameters {
                  withEntity {
                    requestBuilder.addHeader("Content-Type", "application/json")
                  }(body)
                }(parameters)
              }(headers)

            val client = HttpClientBuilder.create.build
            client.execute(request.build)
          }

  def generateUrl(region: String,
                  restApiId: String,
                  stageName: String,
                  path: String,
                  pathWithQuerys: Seq[(String, String)]) = {
    val p = (path /: pathWithQuerys)((p, q) => p.replaceAll(s"\\{${q._1}\\}", q._2))
    s"https://$restApiId.execute-api.$region.amazonaws.com/$stageName$p"
  }

  def request(url: String,
              method: String,
              headers: Seq[(String, String)],
              parameters: Seq[(String, String)],
              body: Option[Array[Byte]]) = {
    doRequest(RequestBuilder.create(method).setUri(url))(headers)(parameters)(body)
  }

  def get(url: String,
          headers: Seq[(String, String)],
          parameters: Seq[(String, String)]) =
    doRequest(RequestBuilder.get(url))(headers)(parameters)(None)

  def post(url: String,
           headers: Seq[(String, String)],
           parameters: Seq[(String, String)],
           body: Array[Byte]) =
    doRequest(RequestBuilder.post(url))(headers)(parameters)(Some(body))

  def put(url: String,
          headers: Seq[(String, String)],
          parameters: Seq[(String, String)],
          body: Array[Byte]) =
    doRequest(RequestBuilder.put(url))(headers)(parameters)(Some(body))

}
