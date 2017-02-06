package com.github.chaabaj.openid

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import com.github.chaabaj.openid.exceptions.{MalformedResponseException, WebServiceException}
import com.github.chaabaj.openid.protocol.JsonProtocol
import spray.json.JsValue

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private [openid] class WebServiceApi(implicit actorSystem: ActorSystem, timeout: FiniteDuration) {
  implicit val materializer = ActorMaterializer()
  val protocol = new JsonProtocol
  val http = Http()

  def request(httpRequest: HttpRequest)(implicit exc: ExecutionContext): Future[JsValue] =
    for {
      response <- http.singleRequest(httpRequest)
      body <- response.entity.toStrict(timeout).map(_.data.decodeString("utf8"))
      data <- {
        protocol.parse(body) match {
          case Success(data) =>
            if (response.status.isFailure()) {
              Future.failed(WebServiceException(response.status, data))
            } else {
              Future.successful(data)
            }
          case Failure(ex) => Future.failed(MalformedResponseException(response.status, ex.toString))
        }
      }
    } yield data
}

private[openid] object WebServiceApi {
  def apply()(implicit system: ActorSystem, _timeout: FiniteDuration): WebServiceApi =
    new WebServiceApi()
}