package note

import akka.actor.{ActorRef, ActorSystem, Props}
import note.mining.MiningSettings
import note.model._
import scorex.core.serialization.Serializer
import scorex.core.settings.ScorexSettings
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.utils.NetworkTimeProvider
import scorex.core.{ModifierId, ModifierTypeId, NodeViewHolder, NodeViewModifier}

import scala.util.{Failure, Success}

/**
  * NCNodeViewHolder Class
  *
  * @param settings
  * @param minerSettings
  * @param timeProvider
  */
class NCNodeViewHolder(settings: ScorexSettings, minerSettings: MiningSettings, timeProvider: NetworkTimeProvider)
      extends NodeViewHolder[PublicKey25519Proposition, BaseEvent, NCBlock] {

  override val networkChunkSize: Int = settings.network.networkChunkSize

  override type SI = NCSyncInfo
  override type HIS = NCBlockChain
  override type MS = NCState
  override type VL = NCWallet
  override type MP = NCCommandMemPool

  override val modifierSerializers: Map[ModifierTypeId, Serializer[_ <: NodeViewModifier]] = Map(
    NCBlock.ModifierTypeId -> NCBlockSerializer,
    BoxInitializerEvent.ModifierTypeId -> BoxInitializerEventSerializer,
    CreateNoteChainEvent.ModifierTypeId -> CreateNoteChainEventSerializer
  )

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    reason.printStackTrace()
    System.exit(100) // this actor shouldn't be restarted at all so kill the whole app if that happened
  }

  /**
   * Hard-coded initial view all the honest nodes in a network are making progress from.
   */
  override protected def genesisState: (HIS, MS, VL, MP) = {
    val emptyBlockchain = new NCBlockChain()
    val emptyState = new NCState

    //each node should use different seed
    val ncWallet = NCWallet(settings.wallet.seed)
    val genesisSecret = ncWallet.secrets.head
    val genesisPublic = ncWallet.publicKeys.head
    val IntitialBaseTarget = BaseTarget @@ 15372286700L

    val initialOffers: Seq[BaseEvent] = Seq()

    val genesisBlock: NCBlock = NCBlock(
      ModifierId @@ Array.fill(NCBlock.SignatureLength)(0: Byte), timeProvider.time(), GenerationSignature @@ Array.fill(NCBlock.SignatureLength)(0: Byte), IntitialBaseTarget, genesisPublic, initialOffers
    )
    log.info(s"Genesis block: $genesisBlock")

    val blockchain = emptyBlockchain.append(genesisBlock) match {
      case Failure(f) => throw f
      case Success(newBlockchain) => newBlockchain._1
    }
    require(blockchain.height() == 1, s"${blockchain.height()} == 1")

    val state = emptyState.applyModifier(genesisBlock) match {
      case Failure(f) => throw f
      case Success(newState) => newState
    }
    require(!state.isEmpty)

    (blockchain, state, NCWallet(settings.wallet.seed), NCCommandMemPool.emptyPool)
  }

  /**
   * Restore a local view during a node startup. If no any stored view found
   * (e.g. if it is a first launch of a node) None is to be returned
   */
  // for now we always have clean start
  override def restoreState(): Option[(HIS, MS, VL, MP)] = None
}

/**
  * NCNodeViewHolderRef Object
  */
object NCNodeViewHolderRef {
  def props(settings: ScorexSettings,
    minerSettings: MiningSettings,
    timeProvider: NetworkTimeProvider): Props = {
    Props(new NCNodeViewHolder(settings, minerSettings, timeProvider))
  }
  def apply(
    settings: ScorexSettings,
    minerSettings: MiningSettings,
    timeProvider: NetworkTimeProvider)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(settings, minerSettings, timeProvider))
  }
  def apply(
    name: String,
    settings: ScorexSettings,
    minerSettings: MiningSettings,
    timeProvider: NetworkTimeProvider)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(settings, minerSettings, timeProvider), name)
  }
}