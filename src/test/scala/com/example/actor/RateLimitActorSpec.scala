/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.actor

//#plain-spec
import akka.actor.ActorSystem
import akka.testkit.{EventFilter, ImplicitSender, TestActors, TestKit, TestProbe}
import com.jyo.hotel.actor.HotelRegistryActor.SendHotelData
import com.jyo.hotel.actor.RateLimitActor
import com.jyo.hotel.actor.RateLimitActor.GetHotels
import com.jyo.hotel.models.CityQuery
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.DurationLong

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
      val cityQuery = CityQuery("Bangkok","1", None)
      rateLimitActor ! GetHotels(cityQuery)
      testProbe.expectMsg(SendHotelData(cityQuery, self))
    }

    "reply as None if rateLimit exceeds for the request" in {
      val testProbe = TestProbe()
      val rateLimitActor = system.actorOf(RateLimitActor.props(testProbe.ref))
      val cityQuery = CityQuery("Bangkok","1",None)
      (1 to 11).foreach(_ => {rateLimitActor ! GetHotels(cityQuery)})
      expectMsg(None)
    }

    "receive tick collect message at 1 sec after sending a request" in {
      val testProbe = TestProbe()
      val rateLimitActor = system.actorOf(RateLimitActor.props(testProbe.ref))
      val cityQuery = CityQuery("Bangkok","1",None)
      rateLimitActor ! GetHotels(cityQuery)
      EventFilter.debug(message = "gets a collect tick message for 1").assertDone(2 second)
    }
  }
}
//#plain-spec