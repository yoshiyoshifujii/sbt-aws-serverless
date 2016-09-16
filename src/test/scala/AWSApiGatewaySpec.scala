import com.amazonaws.services.apigateway.model.PutMode
import com.github.yoshiyoshifujii.aws.apigateway._
import com.typesafe.config.ConfigFactory
import org.scalatest._
import sbt._

import scala.util.{Failure, Success}

class AWSApiGatewaySpec extends FlatSpec with BeforeAndAfterAll with Matchers {

  val RestApiName = "sample-api"
  var RestApiId: String = ""

  override def beforeAll(): Unit = {
    RestApiId = new WithFixture {}.create(RestApiName, Some("hoge")).get.getId
    println(s"create rest api: $RestApiId")
  }

  override def afterAll(): Unit = {
    new WithFixture {}.delete(RestApiId).get
  }

  "AWSApiGatewayRestApiWrapper" should "success" in new WithFixture {
    val restApi = get(RestApiId).get.get
    assert(restApi.getName === RestApiName)
    assert(restApi.getDescription === "hoge")
  }

  trait WithFixture extends AWSApiGatewayRestApiWrapper {
    override val regionName = "us-east-1"
  }

  "Uri" should "success" in {
    assert(Uri("us-east-1", "hoge", "SampleLambda", None).value === "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1:hoge:function:SampleLambda/invocations")
    assert(Uri("us-east-1", "hoge", "SampleLambda", Some("${stageVariables.env}")).value === "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1:hoge:function:SampleLambda:${stageVariables.env}/invocations")
  }

  "AWSApiGatewayMethodsWrapper" should "success getResource" in new WithFixture2 {
    override val restApiId = RestApiId
    override val path = "/hogehoge"
    override val httpMethod = "GET"
    assert(getResource.get.isDefined === false)
  }

  it should "success" in new WithFixture2 {
    override val restApiId = RestApiId
    override val path = "/hellos"
    override val httpMethod = "GET"

    val lambdaName = "sampleLambda1"
    val p = put(restApiId, new File("src/test/resources/swagger01.yaml")).get
    assert(p.getName === "sample-api-2")

    val resource = getResource.get.get

    val resourceId = resource.getId

    putIntegration(
      resourceId = resourceId,
      uri = Uri(regionName, AwsAccountId, lambdaName, Some("${stageVariables.env}")),
      requestTemplates = RequestTemplates("application/json" ->
        """{"stage":{"env":"$stageVariables.env","region":"$stageVariables.region"},"company_id":"$input.params('company-id')","body":$input.json('$')}""")
    ) match {
      case Success(s) =>
        assert(s.getUri === s"arn:aws:apigateway:$regionName:lambda:path/2015-03-31/functions/arn:aws:lambda:$regionName:$AwsAccountId:function:$lambdaName:$${stageVariables.env}/invocations")

        putIntegrationResponse(
          resourceId = resourceId,
          statusCode = "200",
          selectionPattern = None
        ).isSuccess shouldBe true

        putIntegrationResponse(
          resourceId = resourceId,
          statusCode = "500",
          selectionPattern = Some(""".*"statusCode":500.*"""),
          "application/json" ->
            """#set ($errorMessageObj = $util.parseJson($input.path('$.errorMessage')))
              |$util.base64Decode($errorMessageObj.error)""".stripMargin
        ).isSuccess shouldBe true

      case Failure(e) =>
        e.printStackTrace()
        fail(e)
    }
  }

  it should "success deploy" in new WithFixture2 {
    override val restApiId = RestApiId
    override val path = "/hellos"
    override val httpMethod = "GET"
    val lambdaName = "sampleLambda1"
    val p = put(restApiId, new File("src/test/resources/swagger01.yaml")).get
    assert(p.getName === "sample-api-2")

    val uri = Uri(regionName, AwsAccountId, lambdaName, Some("${stageVariables.env}"))

    deploy(
      uri = uri,
      requestTemplates = RequestTemplates("application/json" ->
        """{"stage":{"env":"$stageVariables.env","region":"$stageVariables.region"},"company_id":"$input.params('company-id')","body":$input.json('$')}"""),
      responseTemplates = ResponseTemplates(
        ResponseTemplate("200", None),
        ResponseTemplate("500", Some(""".*"statusCode":500.*"""),
          "application/json" ->
            """#set ($errorMessageObj = $util.parseJson($input.path('$.errorMessage')))
              |$util.base64Decode($errorMessageObj.error)""".stripMargin
        )
      )
    ) match {
      case Success(s) =>
        s.isDefined shouldBe true

        val i = getIntegration(s.get.getId).get.get
        assert(i.getUri === uri.value)
        assert(i.getRequestTemplates.get("application/json") === """{"stage":{"env":"$stageVariables.env","region":"$stageVariables.region"},"company_id":"$input.params('company-id')","body":$input.json('$')}""")
        val ir200 = i.getIntegrationResponses.get("200")
        assert(ir200.getStatusCode === "200")
        assert(ir200.getSelectionPattern === null)
        val ir500 = i.getIntegrationResponses.get("500")
        assert(ir500.getStatusCode === "500")
        assert(ir500.getSelectionPattern === """.*"statusCode":500.*""")
      case Failure(e) =>
        e.printStackTrace()
        fail(e)
    }
  }

  trait WithFixture2 extends AWSApiGatewayMethodsWrapper {
    override val regionName = "us-east-1"
    lazy val config = ConfigFactory.load
    lazy val AwsAccountId = config.getString("aws.account.id")
    lazy val AwsRoleName = config.getString("aws.role.name")

    lazy val fixtures = new WithFixture {}

    lazy val put = (restApiId: String, file: File) =>
      fixtures.put(
        restApiId = restApiId,
        body = file,
        mode = PutMode.Overwrite,
        failOnWarnings = Some(true)
      )
  }
}
