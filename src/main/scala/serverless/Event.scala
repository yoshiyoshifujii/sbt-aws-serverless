package serverless

import com.amazonaws.services.lambda.model.EventSourcePosition

trait Event

case class HttpEvent(path: String,
                     method: String,
                     uriLambdaAlias: String = "${stageVariables.env}",
                     cors: Boolean = false,
                     `private`: Boolean = false,
                     authorizerName: Option[String] = None,
                     request: Request = Request(),
                     invokeInput: Option[HttpInvokeInput] = None) extends Event {
  lazy val response: Response = Response(cors)
}

object HttpEvent {
  def apply(path: String,
            method: String,
            cors: Boolean,
            authorizerName: String,
            invokeInput: HttpInvokeInput): HttpEvent =
    new HttpEvent(
      path = path,
      method = method,
      cors = cors,
      authorizerName = Option(authorizerName),
      invokeInput = Option(invokeInput)
    )
}

case class AuthorizeEvent(name: String,
                          uriLambdaAlias: String = "${stageVariables.env}",
                          resultTtlInSeconds: Int = 1800,
                          identitySourceHeaderName: String = "Authorization",
                          identityValidationExpression: Option[String] = None) extends Event

case class StreamEvent(name: String,
                       batchSize: Int = 100,
                       startingPosition: StartingPosition = StartingPosition.TRIM_HORIZON,
                       enabled: Boolean = true) extends Event {

  def appendToTheNameSuffix(stage: String) = s"$name-$stage"
}

sealed abstract class StartingPosition(val value: EventSourcePosition)

object StartingPosition {
  case object TRIM_HORIZON extends StartingPosition(EventSourcePosition.TRIM_HORIZON)
  case object LATEST extends StartingPosition(EventSourcePosition.LATEST)
  case object AT_TIMESTAMP extends StartingPosition(EventSourcePosition.AT_TIMESTAMP)
}

case class Events(events: Event*) {

  lazy val httpEvents: Seq[HttpEvent] =
    events.filter(_.isInstanceOf[HttpEvent])
      .map(_.asInstanceOf[HttpEvent])

  def httpEventsMap[B](f: HttpEvent => B): Seq[B] = httpEvents map f

  lazy val authorizeEvents: Seq[AuthorizeEvent] =
    events.filter(_.isInstanceOf[AuthorizeEvent])
      .map(_.asInstanceOf[AuthorizeEvent])

  def authorizeEventsMap[B](f: AuthorizeEvent => B): Seq[B] = authorizeEvents map f

  lazy val streamEvents: Seq[StreamEvent] =
    events.filter(_.isInstanceOf[StreamEvent])
      .map(_.asInstanceOf[StreamEvent])

  def streamEventsMap[B](f: StreamEvent => B): Seq[B] = streamEvents map f

  lazy val hasHttpEvent: Boolean = httpEvents.nonEmpty

  lazy val hasAuthorizeEvent: Boolean = authorizeEvents.nonEmpty

  lazy val hasStreamEvent: Boolean = streamEvents.nonEmpty

  def ifHasHttpEventDo[A](f: () => A): Option[() => A] = if (hasHttpEvent) Some(f) else None

  def ifHasAuthorizeEventDo[A](f: () => A): Option[() => A] = if (hasAuthorizeEvent) Some(f) else None

  def ifHasStreamEventDo[A](f: () => A): Option[() => A] = if (hasStreamEvent) Some(f) else None
}

object Events {
  def empty: Events = Events()
}
