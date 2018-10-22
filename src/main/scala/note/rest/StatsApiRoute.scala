package note.rest

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.server.Route
import io.circe.syntax._
import scorex.core.api.http.{ApiRouteWithFullView, SuccessApiResponse}
import scorex.core.settings.RESTApiSettings
import scorex.crypto.encode.Base58
import note.NCCommandMemPool
import note.model.{NCBlockChain, NCState, NCWallet}

/**
  * StatsApiRoute Case Class
  *
  * @param settings
  * @param nodeViewHolderRef
  * @param context
  */
case class StatsApiRoute(override val settings: RESTApiSettings,
                         nodeViewHolderRef: ActorRef)(implicit val context: ActorRefFactory)
           extends ApiRouteWithFullView[NCBlockChain, NCState, NCWallet, NCCommandMemPool] {

  override val route = (pathPrefix("stats") & withCors) {
    tail
  }

  def tail: Route = (get & path("tail" / IntNumber)) { count =>
    withNodeView { view =>
      val lastBlockIds = view.history.lastBlockIds(count)
      val tail = lastBlockIds.map(id => Base58.encode(id).asJson)
      complete(SuccessApiResponse("count" -> count.asJson, "tail" -> tail.asJson))
    }
  }
}