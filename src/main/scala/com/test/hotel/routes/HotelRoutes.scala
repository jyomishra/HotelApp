package com.test.hotel.routes

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.test.hotel.actor.RateLimitActor.GetHotels
import com.test.hotel.models.{ CityQuery, Hotels }
import com.test.hotel.serialization.HotelJsonSupport

import scala.concurrent.Future
import scala.concurrent.duration._

trait HotelRoutes extends HotelJsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[HotelRoutes])

  // other dependencies that UserRoutes use
  def rateLimitActorRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  //#all-routes
  lazy val hotelRoutes: Route =
    pathPrefix("hotels") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[CityQuery]) { city =>
                {
                  if (city.apiKey.isEmpty || city.cityName.isEmpty) {
                    complete((StatusCodes.BadRequest, "Required Parameter Missing"))
                  } else {
                    val hotelFetched: Future[Option[Hotels]] =
                      (rateLimitActorRef ? GetHotels(city)).mapTo[Option[Hotels]]
                    onSuccess(hotelFetched) {
                      case None => complete(StatusCodes.TooManyRequests)
                      case Some(hotels) => {
                        log.info("Fetched hotels with size : {}", hotels.hotels.size)
                        complete((StatusCodes.OK, hotels))
                      }
                    }
                  }
                }
              }
            }
          )
        }
      )
    }
  //#all-routes
}
