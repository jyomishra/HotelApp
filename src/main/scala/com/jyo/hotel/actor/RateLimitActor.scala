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
/*
  Rate Limit actor implements rate limit via sliding window of 10 sec
 */
class RateLimitActor(hotelRegistryActorRef: ActorRef) extends Actor {

  lazy val log = Logging(context.system, classOf[RateLimitActor])

  private val config = ConfigFactory.load()
  // sliding window of size 10
  private var queryCountWindowMap = mutable.Map[String, List[Int]]()
  // store count of request in last 1 second
  private var queryCountTickMap = mutable.Map[String, Int]()
  private var suspendedKeysList = List[String]()
  implicit val ec = Implicits.global

  override def receive = {
    case GetHotels(cityQuery) => {

      // sliding window implementation for rate limit
      log.debug("request received for api key : " + cityQuery.apiKey + " at actor " + self)
      val rateLimit = getRateLimitForApiKey(cityQuery)

      // checks the apiKey in suspended keys list
      if (suspendedKeysList.contains(cityQuery.apiKey)) {
        sender() ! None
      } else {
        // if window map contains the key then
        // check all request count in last 10 sec
        if (queryCountWindowMap.contains(cityQuery.apiKey)) {
          if (queryCountWindowMap(cityQuery.apiKey).sum >= rateLimit || queryCountTickMap(cityQuery.apiKey) >= rateLimit
            || queryCountWindowMap(cityQuery.apiKey).sum + queryCountTickMap(cityQuery.apiKey) >= rateLimit) {
            log.info("Rate limit exceed. Api key suspended : " + cityQuery.apiKey)
            suspendedKeysList ++= List(cityQuery.apiKey)
            context.system.scheduler.scheduleOnce(5 minute, self, RemoveSuspended(cityQuery.apiKey))
            sender() ! None
          } else {
            // if rate is under limit then
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
    // Collects all the request count for apiKey in one second and store in window
    case CollectTick(apiKey) => {
      log.debug("gets a collect tick message for " + apiKey)
      if (queryCountWindowMap.contains(apiKey))
        queryCountWindowMap(apiKey) = (List(queryCountTickMap(apiKey)) ::: queryCountWindowMap(apiKey)).take(10)
      else
        queryCountWindowMap(apiKey) = List(queryCountTickMap(apiKey))
      queryCountTickMap(apiKey) = 0
    }
    // Removes the apiKey from suspended list and reset the counters for the apiKey
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
