package serverless

case class Authorizer(name: String,
                      arn: String,
                      resultTtlInSeconds: Int = 1800,
                      identitySourceHeaderName: String = "Authorization",
                      identityValidationExpression: Option[String] = None)

