package com.jyo.hotel.routes

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ as, concat, entity, onSuccess, pathEnd, pathPrefix, pathSuffix }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import com.jyo.hotel.actor.RateLimitActor.GetHotels
import com.jyo.hotel.models.{ CityQuery, HotelResponse, Hotels }
import com.jyo.hotel.serialization.HotelJsonSupport

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

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
    pathPrefix("v1") {
      concat(
        pathSuffix("hotels") {
          concat(
            post {
              entity(as[CityQuery]) { city =>
                {
                  if (city.apiKey.isEmpty || city.cityName.isEmpty) {
                    complete((StatusCodes.BadRequest, "Required Parameter Missing"))
                  } else if (city.sort.isDefined && !(city.sort.get.equalsIgnoreCase("asc")
                    || city.sort.get.equalsIgnoreCase("desc"))) {
                    complete((StatusCodes.BadRequest, "Sort can be Asc(ascending) or Desc(descending)."))
                  } else {
                    val hotelFetched: Future[Option[Hotels]] =
                      (rateLimitActorRef ? GetHotels(city)).mapTo[Option[Hotels]]
                    onSuccess(hotelFetched) {
                      case None => complete(StatusCodes.TooManyRequests)
                      case Some(hotels) => {
                        log.info("Fetched hotels with size : {}", hotels.hotels.size)
                        if (hotels.hotels.isEmpty) complete((StatusCodes.OK, HotelResponse("No hotel found for city : " + city.cityName, hotels.hotels)))
                        else complete((StatusCodes.OK, HotelResponse("Ok", hotels.hotels)))
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
