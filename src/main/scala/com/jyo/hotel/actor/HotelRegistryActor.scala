package com.jyo.hotel.actor

import java.nio.charset.{ Charset, CodingErrorAction }

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import com.jyo.hotel.models.{ CityQuery, Hotel, HotelObj, Hotels }
import com.typesafe.config.ConfigFactory

import scala.io.Source

object HotelRegistryActor {
  case class SendHotelData(cityQuery: CityQuery, actorRef: ActorRef)
  def props = Props[HotelRegistryActor]
}

class HotelRegistryActor extends Actor {
  lazy val log = Logging(context.system, classOf[HotelRegistryActor])

  import com.jyo.hotel.actor.HotelRegistryActor._
  private val charsetDecoder = Charset.forName("UTF-8").newDecoder
  charsetDecoder.onMalformedInput(CodingErrorAction.IGNORE)
  private val config = ConfigFactory.load()
  private val fileName = config.getString("hotel.inputFile")

  private val hotels: Seq[Hotel] = Source.fromFile(fileName)(charsetDecoder).getLines().map(_.split(",")
    .map(_.trim)).drop(1).map(HotelObj.fromStringArray).toSeq

  def receive: Receive = {
    case SendHotelData(cityQuery, actorRef) => {
      val searchedHotels = hotels.filter(_.city.equalsIgnoreCase(cityQuery.cityName))
      if (cityQuery.sort.isEmpty || cityQuery.sort.get.isEmpty)
        actorRef ! Some(Hotels(searchedHotels))
      else if (cityQuery.sort.get.equalsIgnoreCase("Asc"))
        actorRef ! Some(Hotels(searchedHotels.sortBy(_.price)))
      else
        actorRef ! Some(Hotels(searchedHotels.sortBy(_.price)(Ordering.Int.reverse)))
    }
  }
}

