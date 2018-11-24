package com.test.hotel.serialization

import com.test.hotel.models.{ CityQuery, Hotel, Hotels }

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait HotelJsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val hotelJsonFormat = jsonFormat4(Hotel)
  implicit val hotelsJsonFormat = jsonFormat1(Hotels)
  implicit val cityJsonFormat = jsonFormat3(CityQuery)
}
//#json-support
