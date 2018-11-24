/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.actor

//#plain-spec
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import com.test.hotel.actor.HotelRegistryActor.SendHotelData
import com.test.hotel.actor.RateLimitActor
import com.test.hotel.actor.RateLimitActor.GetHotels
import com.test.hotel.models.CityQuery
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

//#implicit-sender
class RateLimitActorSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
  //#implicit-sender

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An Rate Limit actor" must {

    "forward request to hotelRegistryActor if rateLimit is not exceeded" in {
      val testProbe = TestProbe()
      val rateLimitActor = system.actorOf(RateLimitActor.props(testProbe.ref))
      val cityQuery = CityQuery("Bangkok","1",true)
      rateLimitActor ! GetHotels(cityQuery)
      testProbe.expectMsg(SendHotelData(cityQuery, self))
    }

    "reply as None if rateLimit exceeds for the request" in {
      val testProbe = TestProbe()
      val rateLimitActor = system.actorOf(RateLimitActor.props(testProbe.ref))
      val cityQuery = CityQuery("Bangkok","1",true)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      rateLimitActor ! GetHotels(cityQuery)
      expectMsg(None)
    }

    "receive tick collect message after sending a request" in {
      val testProbe = TestProbe()
      val rateLimitActor = system.actorOf(RateLimitActor.props(testProbe.ref))
      val cityQuery = CityQuery("Bangkok","1",true)
      rateLimitActor ! GetHotels(cityQuery)

    }
  }
}
//#plain-spec