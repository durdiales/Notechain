package note.rest

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.server.Route
import io.circe.parser.parse
import io.circe.syntax._
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.api.http.{ApiException, ApiRouteWithFullView, SuccessApiResponse}
import scorex.core.settings.RESTApiSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import note.NCCommandMemPool
import note.model._
import io.circe.syntax._
import scorex.crypto.encode.Base58

import scala.util.{Failure, Success, Try}

/**
  * NoteChainApiRoute Case Class
  *
  * @param settings
  * @param nodeViewHolderRef
  * @param context
  */
case class NoteChainApiRoute(override val settings: RESTApiSettings,
                             nodeViewHolderRef: ActorRef)(implicit val context: ActorRefFactory)
           extends ApiRouteWithFullView[NCBlockChain, NCState, NCWallet, NCCommandMemPool] {

  override val route = (pathPrefix("note") & withCors) {
    getAll ~ getOne ~ create
  }

  def getAll: Route = (get & pathEndOrSingleSlash) {
    withNodeView { view =>
      val noteGroups = view.state.storage.map(_._2).filter(_.isForgerBox == false)
      complete(SuccessApiResponse(
        "count" -> noteGroups.size.asJson,
        "noteGroups" -> noteGroups.map(_.json).asJson
      ))
    }
  }

  def getOne: Route = (get & path(Segment)) { id =>
    withNodeView { view =>
      val noteGroups = view.state.storage.map(_._2).filter(b => b.isForgerBox == false && Base58.encode(b.id) == id).headOption
      complete(SuccessApiResponse(
        "noteGroup" -> noteGroups.map(_.json).asJson
      ))
    }
  }

  def create: Route = (post) {
    entity(as[String]) { body =>
      withNodeView { view =>
        parse(body) match {
          case Left(failure) => complete(ApiException(failure.getCause))
          case Right(json) => Try {
            val title: String = (json \\ "title").headOption.flatMap(_.asString).get
            val x = (json \\ "notes").head.as[List[String]]
            val tasks: Seq[String] = x.right.get
            val secret = view.vault.secrets.head
            val public = view.vault.publicKeys.head
            val createEvent = CreateNoteChainEvent(public, secret, title, tasks)
            nodeViewHolderRef ! LocallyGeneratedTransaction[PublicKey25519Proposition, CreateNoteChainEvent](createEvent)
            createEvent.json
          } match {
            case Success(resp) => complete(SuccessApiResponse(resp))
            case Failure(e) => complete(ApiException(e))
          }
        }
      }
    }
  }
}
