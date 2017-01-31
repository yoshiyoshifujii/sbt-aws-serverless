package serverless

import com.github.yoshiyoshifujii.aws.apigateway.{ResponseTemplate, ResponseTemplates}

case class Response(statusCodes: ResponseTemplates)

object Response {

  import ResponseTemplatePatterns._

  def apply(cors: Boolean): Response = new Response(
    ResponseTemplates(
      Response200(cors),
      Response204(cors),
      Response400(cors),
      Response401(cors),
      Response403(cors),
      Response404(cors),
      Response408(cors),
      Response500(cors)
    )
  )

}

object ResponseTemplatePatterns {

  private lazy val responseErrorTemplate =
    """#set ($errorMessageObj = $util.parseJson($input.path('$.errorMessage')))
      |$util.base64Decode($errorMessageObj.error)""".stripMargin

  val corsMap = (cors: Boolean) => if (cors) Map("Access-Control-Allow-Origin" -> "'*'") else Map.empty[String, String]

  val Response200 = (cors: Boolean) => ResponseTemplate("200", parameters = corsMap(cors))
  val Response204 = (cors: Boolean) => ResponseTemplate("204", parameters = corsMap(cors))
  val Response400 = (cors: Boolean) => ResponseTemplate("400", Some(""".*"statusCode":400.*"""), Map("application/json" -> responseErrorTemplate), parameters = corsMap(cors))
  val Response401 = (cors: Boolean) => ResponseTemplate("401", Some(""".*"statusCode":401.*"""), Map("application/json" -> responseErrorTemplate), parameters = corsMap(cors))
  val Response403 = (cors: Boolean) => ResponseTemplate("403", Some(""".*"statusCode":403.*"""), Map("application/json" -> responseErrorTemplate), parameters = corsMap(cors))
  val Response404 = (cors: Boolean) => ResponseTemplate("404", Some(""".*"statusCode":404.*"""), Map("application/json" -> responseErrorTemplate), parameters = corsMap(cors))
  val Response408 = (cors: Boolean) => ResponseTemplate("408", Some(""".*Task timed out.*"""), parameters = corsMap(cors))
  val Response500 = (cors: Boolean) => ResponseTemplate("500", Some(""".*"statusCode":500.*"""), Map("application/json" -> responseErrorTemplate), parameters = corsMap(cors))

}

