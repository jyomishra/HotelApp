package com.jyo.hotel.models

case class Hotel(city: String, hotelId: Int, room: String, price: Int)

case class Hotels(hotels: Seq[Hotel])

case class CityQuery(cityName: String, apiKey: String, sort: Option[String])

case class HotelResponse(message: String, hotels: Seq[Hotel])

object HotelObj {
  def fromStringArray(values: Array[String]) = Hotel(values(0), values(1).toInt, values(2), values(3).toInt)
}