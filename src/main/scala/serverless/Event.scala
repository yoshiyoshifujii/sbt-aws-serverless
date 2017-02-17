package serverless

trait Event

case class HttpEvent(path: String,
                     method: String,
                     uriLambdaAlias: String = "${stageVariables.env}",
                     cors: Boolean = false,
                     `private`: Boolean = false,
                     authorizerName: Option[String] = None,
                     request: Request = Request()) extends Event {
  lazy val response: Response = Response(cors)
}

case class AuthorizeEvent(name: String,
                          uriLambdaAlias: String = "${stageVariables.env}",
                          resultTtlInSeconds: Int = 1800,
                          identitySourceHeaderName: String = "Authorization",
                          identityValidationExpression: Option[String] = None) extends Event

case class StreamEvent(arn: String,
                       batchSize: Int = 100,
                       startingPosition: String,
                       enabled: Boolean = false) extends Event

case class Events(events: Event*) {

  private lazy val httpEvents = events.filter(_.isInstanceOf[HttpEvent])

  def httpEventsMap[B](f: HttpEvent => B): Seq[B] =
    httpEvents.map(e => f(e.asInstanceOf[HttpEvent]))

  private lazy val authorizeEvents = events.filter(_.isInstanceOf[AuthorizeEvent])

  def authorizeEventMap[B](f: AuthorizeEvent => B): Seq[B] =
    authorizeEvents.map(e => f(e.asInstanceOf[AuthorizeEvent]))

  private lazy val streamEvents = events.filter(_.isInstanceOf[StreamEvent])

  def streamEventMap[B](f: StreamEvent => B): Seq[B] =
    streamEvents.map(e => f(e.asInstanceOf[StreamEvent]))

  def hasAuthorizeEvent: Boolean = authorizeEvents.nonEmpty

}

object Events {
  def empty: Events = Events()
}
