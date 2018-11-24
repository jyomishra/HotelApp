package com.test.hotel.models

case class Hotel(city: String, hotelId: Int, room: String, price: Int)

case class Hotels(hotels: Seq[Hotel])

case class CityQuery(cityName: String, apiKey: String, sort: Option[String])

object HotelObj {
  def fromStringArray(values: Array[String]) = Hotel(values(0), values(1).toInt, values(2), values(3).toInt)
}