package com.test.hotel

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.routing.{ ConsistentHashingPool, SmallestMailboxPool }
import akka.stream.ActorMaterializer
import com.test.hotel.actor.{ HotelRegistryActor, RateLimitActor }
import com.test.hotel.routes.HotelRoutes
import com.typesafe.config.ConfigFactory

object ServerMain extends App with HotelRoutes {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem(name = "hotel-actor-system", config)
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val host = config.getString("app.host")
  val port = config.getInt("app.port")

  val hotelRegistryActor = system.actorOf(SmallestMailboxPool(5).props(HotelRegistryActor.props), "hotelRegistryActor")
  val rateLimitActorRef = system.actorOf(ConsistentHashingPool(5).props(RateLimitActor.props(hotelRegistryActor)), "rateLimitActor")

  val bindingFuture = Http().bindAndHandle(hotelRoutes, host, port)
  bindingFuture.map(_.localAddress).map(addr => s"Bound to $addr").foreach(log.info)
}
