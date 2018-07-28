package tests.generators.akkaHttp.client

import com.twilio.guardrail.generators.AkkaHttp
import com.twilio.guardrail.{ Client, Clients, Context }
import org.scalatest.{ FunSuite, Matchers }
import support.SwaggerSpecRunner
import com.twilio.guardrail.tests._
import scala.meta._

class SchemeTest extends FunSuite with Matchers with SwaggerSpecRunner {

  val swagger: String = s"""
    |swagger: "2.0"
    |info:
    |  title: Whatever
    |  version: 1.0.0
    |host: localhost:1234
    |schemes:
    |  - https
    |paths:
    |  /foo:
    |    get:
    |      operationId: getFoo
    |      responses:
    |        200:
    |          schema:
    |            $$ref: "#/definitions/Bar"
    |definitions:
    |  Bar:
    |    type: object
    |    properties:
    |      id:
    |        type: integer
    |        format: int64
    |""".stripMargin

  test("Use first scheme") {
    val (_, Clients(Client(_, _, statements) :: _), _) =
      runSwaggerSpec(swagger)(Context.empty, AkkaHttp, defaults.akkaGeneratorSettings)

    val List(cmp, cls) = statements.dropWhile(_.isInstanceOf[Import])

    val companion = q"""
    object Client {
      def apply(host: String = "https://localhost:1234")(implicit httpClient: HttpRequest => Future[HttpResponse], ec: ExecutionContext, mat: Materializer): Client = new Client(host = host)(httpClient = httpClient, ec = ec, mat = mat)
      def httpClient(httpClient: HttpRequest => Future[HttpResponse], host: String = "https://localhost:1234")(implicit ec: ExecutionContext, mat: Materializer): Client = new Client(host = host)(httpClient = httpClient, ec = ec, mat = mat)
    }
    """

    val client = q"""
      class Client(host: String = "https://localhost:1234")(implicit httpClient: HttpRequest => Future[HttpResponse], ec: ExecutionContext, mat: Materializer) {
        val basePath: String = ""
        private[this] def makeRequest[T: ToEntityMarshaller](method: HttpMethod, uri: Uri, headers: scala.collection.immutable.Seq[HttpHeader], entity: T, protocol: HttpProtocol): EitherT[Future, Either[Throwable, HttpResponse], HttpRequest] = {
          EitherT(Marshal(entity).to[RequestEntity].map[Either[Either[Throwable, HttpResponse], HttpRequest]] {
            entity => Right(HttpRequest(method = method, uri = uri, headers = headers, entity = entity, protocol = protocol))
          }.recover({
            case t =>
              Left(Left(t))
          }))
        }
        private[this] def wrap[T: FromEntityUnmarshaller](client: HttpClient, request: HttpRequest): EitherT[Future, Either[Throwable, HttpResponse], T] = {
          EitherT(client(request).flatMap(resp => if (resp.status.isSuccess) {
            Unmarshal(resp.entity).to[T].map(Right.apply _)
          } else {
            FastFuture.successful(Left(Right(resp)))
          }).recover({
            case e: Throwable =>
              Left(Left(e))
          }))
        }
        def getFoo(headers: scala.collection.immutable.Seq[HttpHeader] = Nil): EitherT[Future, Either[Throwable, HttpResponse], Bar] = {
          val allHeaders = headers ++ scala.collection.immutable.Seq[Option[HttpHeader]]().flatten
          makeRequest(HttpMethods.GET, host + basePath + "/foo", allHeaders, HttpEntity.Empty, HttpProtocols.`HTTP/1.1`).flatMap(req => wrap[Bar](httpClient, req))
        }
      }
    """

    cmp.structure should equal(companion.structure)
    cls.structure should equal(client.structure)
  }
}
