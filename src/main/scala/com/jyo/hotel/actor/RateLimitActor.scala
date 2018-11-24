package com.jyo.hotel.actor

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import com.jyo.hotel.actor.HotelRegistryActor.SendHotelData
import com.jyo.hotel.actor.RateLimitActor.{ CollectTick, GetHotels, RemoveSuspended }
import com.jyo.hotel.models.CityQuery
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.DurationLong

object RateLimitActor {
  case class GetHotels(cityQuery: CityQuery) extends ConsistentHashable {
    override def consistentHashKey: Any = cityQuery.apiKey
  }
  case class CollectTick(apiKey: String)
  case class RemoveSuspended(apiKey: String)
  def props(hotelRegistryActorRef: ActorRef) = Props(new RateLimitActor(hotelRegistryActorRef))
}

class RateLimitActor(hotelRegistryActorRef: ActorRef) extends Actor {

  lazy val log = Logging(context.system, classOf[RateLimitActor])

  private val config = ConfigFactory.load()
  private var queryCountWindowMap = mutable.Map[String, List[Int]]()
  private var queryCountTickMap = mutable.Map[String, Int]()
  private var suspendedKeysList = List[String]()
  implicit val ec = Implicits.global

  override def receive = {
    case GetHotels(cityQuery) => {
      log.debug("request received for api key : " + cityQuery.apiKey + " at actor " + self)
      val rateLimit = getRateLimitForApiKey(cityQuery)
      if (suspendedKeysList.contains(cityQuery.apiKey)) {
        sender() ! None
      } else {
        if (queryCountWindowMap.contains(cityQuery.apiKey)) {
          if (queryCountWindowMap(cityQuery.apiKey).sum >= rateLimit || queryCountTickMap(cityQuery.apiKey) >= rateLimit
            || queryCountWindowMap(cityQuery.apiKey).sum + queryCountTickMap(cityQuery.apiKey) >= rateLimit) {
            log.info("Rate limit exceed. Api key suspended : " + cityQuery.apiKey)
            suspendedKeysList ++= List(cityQuery.apiKey)
            context.system.scheduler.scheduleOnce(5 minute, self, RemoveSuspended(cityQuery.apiKey))
            sender() ! None
          } else {
            queryCountTickMap(cityQuery.apiKey) = queryCountTickMap(cityQuery.apiKey) + 1
            hotelRegistryActorRef ! SendHotelData(cityQuery, sender())
          }
        } else {
          queryCountWindowMap(cityQuery.apiKey) = List.empty[Int]
          if (queryCountTickMap.contains(cityQuery.apiKey)) {
            queryCountTickMap(cityQuery.apiKey) = queryCountTickMap(cityQuery.apiKey) + 1
          } else {
            queryCountTickMap(cityQuery.apiKey) = 1
            context.system.scheduler.schedule(1 second, 1 second, self, CollectTick(cityQuery.apiKey))
          }
          hotelRegistryActorRef ! SendHotelData(cityQuery, sender())
        }
      }
    }
    case CollectTick(apiKey) => {
      if (queryCountWindowMap.contains(apiKey))
        queryCountWindowMap(apiKey) = (List(queryCountTickMap(apiKey)) ::: queryCountWindowMap(apiKey)).take(10)
      else
        queryCountWindowMap(apiKey) = List(queryCountTickMap(apiKey))
      queryCountTickMap(apiKey) = 0
    }
    case RemoveSuspended(apiKey) => {
      suspendedKeysList = suspendedKeysList.take(suspendedKeysList.size - 1)
      queryCountTickMap(apiKey) = 0
      queryCountWindowMap(apiKey) = List.empty[Int]
      log.info("Rate limit restored for Api key : " + apiKey)
    }
  }

  private def getRateLimitForApiKey(cityQuery: CityQuery) = {
    // get the rate limit
    val pathPrefix = "hotel.api.rateLimit."
    if (config.hasPath(pathPrefix + cityQuery.apiKey))
      config.getInt(pathPrefix + cityQuery.apiKey)
    else
      config.getInt(pathPrefix + "global")
  }
}
