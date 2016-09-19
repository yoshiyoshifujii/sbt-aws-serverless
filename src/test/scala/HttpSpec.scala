import org.scalatest._

class HttpSpec extends FlatSpec with Matchers {
  import com.github.yoshiyoshifujii.aws.http._

  "http" should "success" in {
    val t = generateUrl(
      region = "us-east-1",
      restApiId = "123456",
      stageName = "test",
      path = "hello/{id}/message/{message}",
      pathWithQuerys = Seq(
        "id" -> "1",
        "message" -> "world"
      )
    )

    assert(t === "https://123456.execute-api.us-east-1.amazonaws.com/test/hello/1/message/world")
  }
}
