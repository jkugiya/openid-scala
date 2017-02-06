package com.github.chaabaj.openid.apis.slack

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import com.github.chaabaj.openid.WebServiceApi
import com.github.chaabaj.openid.exceptions.WebServiceException
import com.github.chaabaj.openid.oauth.AccessTokenSuccess
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class SlackIdentityServiceSpec extends Specification with Mockito {
  private def createService(): SlackIdentityService =
    new SlackIdentityService {
      override val webServiceApi: WebServiceApi = smartMock[WebServiceApi]
    }

  private val duration = 10 seconds

  import scala.concurrent.ExecutionContext.Implicits.global

  "should get identity of user" >> {
    val service = createService()
    val response =
      """
        |{
        |    "ok": true,
        |    "user": {
        |        "name": "Sonny Whether",
        |        "id": "U0G9QF9C6",
        |        "email": "test@test.com"
        |    },
        |    "team": {
        |      "id": "T0G9PQBBK"
        |    }
        |}
      """.stripMargin.parseJson
    val token = AccessTokenSuccess(
      accessToken = "test",
      tokenType = None
    )

    service.webServiceApi.request(any[HttpRequest])(any[ExecutionContext]) returns Future.successful(response)

    val identity = Await.result(service.getIdentity(token), duration)

    identity must equalTo("test@test.com")
  }

  "should fails with a WebServiceException" >> {
    val service = createService()
    val response =
      """
        |{
        |  "ok": false,
        |  "error": "not_authed"
        |}
      """.stripMargin.parseJson
    val token = AccessTokenSuccess(
      accessToken = "test",
      tokenType = None
    )
    val error = WebServiceException(StatusCodes.BadRequest, response)

    service.webServiceApi.request(any[HttpRequest])(any[ExecutionContext]) returns Future.successful(response)

    Await.result(service.getIdentity(token), duration) must throwA[WebServiceException[JsValue]]
  }
}
