package note

import akka.actor.{ActorRef, Props}
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizerRef
import scorex.core.network.message.MessageSpec
import scorex.core.settings.ScorexSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import note.rest.{DebugApiRoute, StatsApiRoute, NoteChainApiRoute}
import note.mining.{AppSettings, NCForger}
import note.model._

import scala.io.Source
import scala.language.postfixOps

/**
  * NoteChainApp Class
  *
  * @param settingsFilename
  */
class NoteChainApp(val settingsFilename: String) extends Application {

  override type P = PublicKey25519Proposition
  override type TX = BaseEvent
  override type PMOD = NCBlock
  override type NVHT = NCNodeViewHolder

  private val hybridSettings = AppSettings.read(Some(settingsFilename))

  implicit override lazy val settings: ScorexSettings = AppSettings.read(Some(settingsFilename)).scorexSettings

  log.debug(s"Starting application with settings \n$settings")

  override protected lazy val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq(NCSyncInfoMessageSpec)

  override val nodeViewHolderRef: ActorRef = NCNodeViewHolderRef(settings, hybridSettings.mining, timeProvider)

  override val apiRoutes: Seq[ApiRoute] = Seq(
    StatsApiRoute(settings.restApi, nodeViewHolderRef),
    DebugApiRoute(settings.restApi, nodeViewHolderRef),
    NoteChainApiRoute(settings.restApi, nodeViewHolderRef),
    NodeViewApiRoute[P, TX](settings.restApi, nodeViewHolderRef)
  )

  /* Swagger Configuration */
  override val swaggerConfig: String = Source.fromResource("api/testApi.yaml").getLines.mkString("\n")

  val forger: ActorRef = actorSystem.actorOf(Props(new NCForger(nodeViewHolderRef, hybridSettings.mining, timeProvider)))

  override val localInterface: ActorRef = NCLocalInterfaceRef(nodeViewHolderRef, forger, hybridSettings.mining)

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(NodeViewSynchronizerRef.props[P, TX, NCSyncInfo, NCSyncInfoMessageSpec.type, PMOD, NCBlockChain, NCCommandMemPool](networkControllerRef, nodeViewHolderRef, localInterface,
      NCSyncInfoMessageSpec, settings.network, timeProvider))
}

/**
  * NoteChainApp Object
  */
object NoteChainApp extends App {
  val settingsFilename = args.headOption.getOrElse("settings.conf")
  new NoteChainApp(settingsFilename).run()
}
