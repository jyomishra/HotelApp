package com.jyo.hotel.serialization

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.jyo.hotel.models.{ CityQuery, Hotel, HotelResponse, Hotels }
import spray.json.DefaultJsonProtocol

trait HotelJsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val hotelJsonFormat = jsonFormat4(Hotel)
  implicit val hotelsJsonFormat = jsonFormat1(Hotels)
  implicit val cityJsonFormat = jsonFormat3(CityQuery)
  implicit val hotelResponseJson = jsonFormat2(HotelResponse)
}
