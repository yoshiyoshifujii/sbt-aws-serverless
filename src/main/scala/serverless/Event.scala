package serverless

import com.amazonaws.services.lambda.model.EventSourcePosition
import com.github.yoshiyoshifujii.aws.dynamodb.AWSDynamoDB
import com.github.yoshiyoshifujii.aws.kinesis.AWSKinesis

import scala.util.Try

trait Event

case class HttpEvent(path: String,
                     method: String,
                     uriLambdaAlias: Option[String] = Some("${stageVariables.env}"),
                     cors: Boolean = false,
                     `private`: Boolean = false,
                     authorizerName: Option[String] = None,
                     request: Request = Request(),
                     invokeInput: Option[HttpInvokeInput] = None)
    extends Event {
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
                          uriLambdaAlias: Option[String] = Some("${stageVariables.env}"),
                          resultTtlInSeconds: Int = 1800,
                          identitySourceHeaderName: String = "Authorization",
                          identityValidationExpression: Option[String] = None)
    extends Event

sealed trait StreamEvent extends Event {
  val name: String
  val batchSize: Int
  val startingPosition: StartingPosition
  val enabled: Boolean
  val oldFunctions: Seq[FunctionBase]

  def appendToTheNameSuffix(stage: String) = s"$name-$stage"

  def getArn(regionName: String, stage: String): Try[String]

  def printDescribe(regionName: String, stage: String): Try[Unit]

}

case class KinesisStreamEvent(name: String,
                              batchSize: Int = 100,
                              startingPosition: KinesisStartingPosition =
                                KinesisStartingPosition.TRIM_HORIZON,
                              enabled: Boolean = true,
                              oldFunctions: Seq[FunctionBase] = Seq.empty)
    extends StreamEvent {
  override def getArn(regionName: String, stage: String) =
    AWSKinesis(regionName)
      .describeStream(appendToTheNameSuffix(stage))
      .map(_.getStreamDescription.getStreamARN)

  override def printDescribe(regionName: String, stage: String) = Try {
    val streamName = appendToTheNameSuffix(stage)
    AWSKinesis(regionName).describeStream(streamName) map { s =>
      println("KinesisStream:")
      println("- " + s.getStreamDescription.getStreamName)
      println("- " + s.getStreamDescription.getStreamStatus)
      println("- " + s.getStreamDescription.getStreamARN)
    } getOrElse {
      s"Not exists. $streamName"
    }
  }
}

case class DynamoDBStreamEvent(name: String,
                               batchSize: Int = 100,
                               startingPosition: DynamoDBStartingPosition =
                                 DynamoDBStartingPosition.TRIM_HORIZON,
                               enabled: Boolean = true,
                               oldFunctions: Seq[FunctionBase] = Seq.empty)
    extends StreamEvent {
  override def getArn(regionName: String, stage: String) =
    AWSDynamoDB(regionName)
      .describeTable(appendToTheNameSuffix(stage))
      .map(_.getTable.getLatestStreamArn)

  override def printDescribe(regionName: String, stage: String): Try[Unit] = Try {
    val tableName = appendToTheNameSuffix(stage)
    AWSDynamoDB(regionName).describeTable(tableName) map { t =>
      println("DynamoDB:")
      println("- " + t.getTable.getTableName)
      println("- " + t.getTable.getTableStatus)
      println("- " + t.getTable.getTableArn)
    } getOrElse {
      println(s"Not exists. $tableName")
    }
  }
}

sealed trait StartingPosition {
  val value: EventSourcePosition
}

sealed abstract class KinesisStartingPosition(val value: EventSourcePosition)
    extends StartingPosition

object KinesisStartingPosition {
  case object TRIM_HORIZON extends KinesisStartingPosition(EventSourcePosition.TRIM_HORIZON)
  case object LATEST       extends KinesisStartingPosition(EventSourcePosition.LATEST)
  case object AT_TIMESTAMP extends KinesisStartingPosition(EventSourcePosition.AT_TIMESTAMP)
}

sealed abstract class DynamoDBStartingPosition(val value: EventSourcePosition)
    extends StartingPosition

object DynamoDBStartingPosition {
  case object TRIM_HORIZON extends DynamoDBStartingPosition(EventSourcePosition.TRIM_HORIZON)
  case object LATEST       extends DynamoDBStartingPosition(EventSourcePosition.LATEST)
}

case class Events(events: Event*) {

  lazy val httpEvents: Seq[HttpEvent] =
    events
      .filter(_.isInstanceOf[HttpEvent])
      .map(_.asInstanceOf[HttpEvent])

  def httpEventsMap[B](f: HttpEvent => B): Seq[B] = httpEvents map f

  lazy val authorizeEvents: Seq[AuthorizeEvent] =
    events
      .filter(_.isInstanceOf[AuthorizeEvent])
      .map(_.asInstanceOf[AuthorizeEvent])

  def authorizeEventsMap[B](f: AuthorizeEvent => B): Seq[B] = authorizeEvents map f

  lazy val streamEvents: Seq[StreamEvent] =
    events
      .filter(_.isInstanceOf[StreamEvent])
      .map(_.asInstanceOf[StreamEvent])

  def streamEventsMap[B](f: StreamEvent => B): Seq[B] = streamEvents map f

  lazy val hasHttpEvent: Boolean = httpEvents.nonEmpty

  lazy val hasAuthorizeEvent: Boolean = authorizeEvents.nonEmpty

  lazy val hasStreamEvent: Boolean = streamEvents.nonEmpty

  def ifHasHttpEventDo[A](f: () => A): Option[() => A] = if (hasHttpEvent) Some(f) else None

  def ifHasNotHttpEventDo[A](f: () => A): Option[() => A] = if (!hasHttpEvent) Some(f) else None

  def ifHasAuthorizeEventDo[A](f: () => A): Option[() => A] =
    if (hasAuthorizeEvent) Some(f) else None

  def ifHasStreamEventDo[A](f: () => A): Option[() => A] = if (hasStreamEvent) Some(f) else None
}

object Events {
  def empty: Events = Events()
}
