package serverless

case class Authorizer(name: String,
                      resultTtlInSeconds: Int = 1800,
                      identitySourceHeaderName: String = "Authorization",
                      identityValidationExpression: Option[String] = None)

