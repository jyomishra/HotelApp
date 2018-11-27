package com.example.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.jyo.hotel.actor.HotelRegistryActor
import com.jyo.hotel.actor.HotelRegistryActor.SendHotelData
import com.jyo.hotel.models.{CityQuery, Hotel, Hotels}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class HotelRegistryActorSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
  //#implicit-sender

  "An Hotel Registry Actor" must {

    "reply to city query with list of hotels without any order" in {
      val testProbe = TestProbe()
      val hotelRegActor = system.actorOf(HotelRegistryActor.props)
      val cityQuery = CityQuery("Bangkok", "1", None)
      hotelRegActor ! SendHotelData(cityQuery, testProbe.ref)
      testProbe.expectMsgType[Some[Hotels]]
    }

    "reply to city query with blank list of hotels if city not found" in {
      val testProbe = TestProbe()
      val hotelRegActor = system.actorOf(HotelRegistryActor.props)
      val cityQuery = CityQuery("ABC", "1", None)
      hotelRegActor ! SendHotelData(cityQuery, testProbe.ref)
      testProbe.expectMsg(Some(Hotels(Stream.empty[Hotel])))
    }
  }

}
