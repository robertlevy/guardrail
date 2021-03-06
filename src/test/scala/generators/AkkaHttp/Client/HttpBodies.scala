package com.twilio.swagger

import _root_.io.swagger.parser.SwaggerParser
import cats.instances.all._
import com.twilio.swagger.codegen.generators.AkkaHttp
import com.twilio.swagger.codegen.{ClassDefinition, Client, Clients, Context, ClientGenerator, ProtocolGenerator, Target, CodegenApplication}
import org.scalatest.{FunSuite, Matchers}
import scala.collection.immutable.{Seq => ISeq}
import scala.meta._

class HttpBodiesTest extends FunSuite with Matchers {
  val swagger = s"""
    |swagger: "2.0"
    |info:
    |  title: Whatever
    |  version: 1.0.0
    |host: localhost:1234
    |schemes:
    |  - http
    |paths:
    |  /foo:
    |    get:
    |      operationId: getFoo
    |      parameters:
    |        - name: body
    |          in: body
    |          required: true
    |          schema:
    |            $$ref: "#/definitions/Foo"
    |      responses:
    |        200:
    |          description: Success
    |    put:
    |      operationId: putFoo
    |      parameters:
    |        - name: body
    |          in: body
    |          required: true
    |          schema:
    |            $$ref: "#/definitions/Foo"
    |      responses:
    |        200:
    |          description: Success
    |    post:
    |      operationId: postFoo
    |      parameters:
    |        - name: body
    |          in: body
    |          required: true
    |          schema:
    |            $$ref: "#/definitions/Foo"
    |      responses:
    |        200:
    |          description: Success
    |    delete:
    |      operationId: deleteFoo
    |      parameters:
    |        - name: body
    |          in: body
    |          required: true
    |          schema:
    |            $$ref: "#/definitions/Foo"
    |      responses:
    |        200:
    |          description: Success
    |    patch:
    |      operationId: patchFoo
    |      parameters:
    |        - name: body
    |          in: body
    |          required: true
    |          schema:
    |            $$ref: "#/definitions/Foo"
    |      responses:
    |        200:
    |          description: Success
    |definitions:
    |  Foo:
    |    type: object
    |    required:
    |      - map
    |    properties:
    |      map:
    |        type: object
    |""".stripMargin

  test("Properly handle all methods") {
    val (
      _,
      Clients(Client(tags, className, statements) :: _),
      _
    ) = runSwaggerSpec(swagger)(Context.empty, AkkaHttp)

    val Seq(cmp, cls) = statements.dropWhile(_.isInstanceOf[Import])

    val client = q"""
      class Client(host: String = "http://localhost:1234")(implicit httpClient: HttpRequest => Future[HttpResponse], ec: ExecutionContext, mat: Materializer) {
        val basePath: String = ""
        private[this] def wrap[T: FromEntityUnmarshaller](resp: Future[HttpResponse]): EitherT[Future, Either[Throwable, HttpResponse], T] = {
          EitherT(resp.flatMap(resp => if (resp.status.isSuccess) {
            Unmarshal(resp.entity).to[T].map(Right.apply _)
          } else {
            FastFuture.successful(Left(Right(resp)))
          }).recover({
            case e: Throwable =>
              Left(Left(e))
          }))
        }
        def getFoo(body: Foo, headers: scala.collection.immutable.Seq[HttpHeader] = Nil): EitherT[Future, Either[Throwable, HttpResponse], IgnoredEntity] = {
          val allHeaders = headers ++ scala.collection.immutable.Seq[Option[HttpHeader]]().flatten
          wrap[IgnoredEntity](Marshal(body).to[RequestEntity].flatMap { entity =>
            httpClient(HttpRequest(method = HttpMethods.GET, uri = host + basePath + "/foo", entity = entity, headers = allHeaders))
          })
        }
        def putFoo(body: Foo, headers: scala.collection.immutable.Seq[HttpHeader] = Nil): EitherT[Future, Either[Throwable, HttpResponse], IgnoredEntity] = {
          val allHeaders = headers ++ scala.collection.immutable.Seq[Option[HttpHeader]]().flatten
          wrap[IgnoredEntity](Marshal(body).to[RequestEntity].flatMap { entity =>
            httpClient(HttpRequest(method = HttpMethods.PUT, uri = host + basePath + "/foo", entity = entity, headers = allHeaders))
          })
        }
        def postFoo(body: Foo, headers: scala.collection.immutable.Seq[HttpHeader] = Nil): EitherT[Future, Either[Throwable, HttpResponse], IgnoredEntity] = {
          val allHeaders = headers ++ scala.collection.immutable.Seq[Option[HttpHeader]]().flatten
          wrap[IgnoredEntity](Marshal(body).to[RequestEntity].flatMap { entity =>
            httpClient(HttpRequest(method = HttpMethods.POST, uri = host + basePath + "/foo", entity = entity, headers = allHeaders))
          })
        }
        def deleteFoo(body: Foo, headers: scala.collection.immutable.Seq[HttpHeader] = Nil): EitherT[Future, Either[Throwable, HttpResponse], IgnoredEntity] = {
          val allHeaders = headers ++ scala.collection.immutable.Seq[Option[HttpHeader]]().flatten
          wrap[IgnoredEntity](Marshal(body).to[RequestEntity].flatMap { entity =>
            httpClient(HttpRequest(method = HttpMethods.DELETE, uri = host + basePath + "/foo", entity = entity, headers = allHeaders))
          })
        }
        def patchFoo(body: Foo, headers: scala.collection.immutable.Seq[HttpHeader] = Nil): EitherT[Future, Either[Throwable, HttpResponse], IgnoredEntity] = {
          val allHeaders = headers ++ scala.collection.immutable.Seq[Option[HttpHeader]]().flatten
          wrap[IgnoredEntity](Marshal(body).to[RequestEntity].flatMap { entity =>
            httpClient(HttpRequest(method = HttpMethods.PATCH, uri = host + basePath + "/foo", entity = entity, headers = allHeaders))
          })
        }
      }
    """

    cls.structure should equal(client.structure)
  }
}
