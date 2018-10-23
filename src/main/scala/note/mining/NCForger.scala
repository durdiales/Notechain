package note.mining

import akka.actor.{Actor, ActorRef}
import note.NCCommandMemPool
import note.model._
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedModifier
import scorex.core.NodeViewHolder.CurrentView
import scorex.core.NodeViewHolder.ReceivableMessages.GetDataFromCurrentView
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import scorex.core.utils.{NetworkTimeProvider, ScorexLogging}
import scorex.crypto.hash.Blake2b256

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * NCForger Class
  *
  * @param viewHolderRef
  * @param forgerSettings
  * @param timeProvider
  */
class NCForger(viewHolderRef: ActorRef, forgerSettings: MiningSettings, timeProvider: NetworkTimeProvider)
      extends Actor
      with ScorexLogging {

  import NCForger._

  //set to true for initial generator
  private var forging = forgerSettings.offlineGeneration

  private val hash = Blake2b256

  override def preStart(): Unit = {
    if (forging) context.system.scheduler.scheduleOnce(1.second)(self ! Forge)
  }

  private def bounded(value: BigInt, min: BigInt, max: BigInt): BigInt = {
    if (value < min) min else if (value > max) max else value
  }

  private def calcBaseTarget(lastBlock: NCBlock, currentTime: Long): Long = {
    val eta = currentTime - lastBlock.timestamp
    val prevBt = BigInt(lastBlock.baseTarget)
    val t0: BigInt = bounded(prevBt * eta / forgerSettings.targetBlockDelay.toMillis, prevBt / 2, prevBt * 2)
    bounded(t0, 1, Long.MaxValue).toLong
  }

  protected def calcTarget(lastBlock: NCBlock, boxOpt: Option[NCBox]): BigInt = {
    val eta = (timeProvider.time() - lastBlock.timestamp) / 1000 //in seconds
    //we are ignoring original Box value
    val balance = boxOpt.map(_ => 50000000L ).getOrElse(0L)
    BigInt(lastBlock.baseTarget) * eta * balance
  }

  private def calcGeneratorSignature(lastBlock: NCBlock, generator: PublicKey25519Proposition) = {
    hash(lastBlock.generationSignature ++ generator.pubKeyBytes)
  }

  private def calcHit(lastBlock: NCBlock, generator: PublicKey25519Proposition): BigInt = {
    BigInt(1, calcGeneratorSignature(lastBlock, generator).take(8))
  }

  override def receive: Receive = {
    case StartMining =>
      forging = true
      context.system.scheduler.scheduleOnce(forgerSettings.blockGenerationDelay)(self ! Forge)

    case StopMining =>
      forging = false

    case info: RequiredForgingInfo =>
      val lastBlock = info.lastBlock
      log.info(s"Trying to generate a new block on top of $lastBlock")
      lazy val toInclude = info.toInclude

      val generatedBlocks = info.gbs.flatMap { gb =>
        val generator = gb._1
        val hit = calcHit(lastBlock, generator)
        val target = calcTarget(lastBlock, gb._2)
        if (hit < target) {
          Some {
            val timestamp = timeProvider.time()
            val bt = BaseTarget @@ calcBaseTarget(lastBlock, timestamp)
            val secret = gb._3
            val gs = GenerationSignature @@ Array[Byte]()

            val unsigned: NCBlock = NCBlock(lastBlock.id, timestamp, gs, bt, generator, toInclude)
            val signature = PrivateKey25519Companion.sign(secret, unsigned.serializer.messageToSign(unsigned))
            val signedBlock = unsigned.copy(generationSignature = GenerationSignature @@ signature.signature)
            log.info(s"Generated new block: ${signedBlock.json.noSpaces}")
            LocallyGeneratedModifier[NCBlock](signedBlock)
          }
        } else {
          None
        }
      }
      generatedBlocks.foreach(localModifier => viewHolderRef ! localModifier)
      context.system.scheduler.scheduleOnce(forgerSettings.blockGenerationDelay)(self ! Forge)

    case Forge =>
      viewHolderRef ! NCForger.getRequiredData
  }
}

/**
  * NCForger Object
  */
object NCForger {

  case class RequiredForgingInfo(
                                  toInclude: Seq[BaseEvent],
                                  lastBlock: NCBlock,
                                  gbs: Seq[(PublicKey25519Proposition, Option[NCBox], PrivateKey25519)])

  case object StartMining
  case object StopMining
  case object Forge

  //should be a part of consensus, but for our app is okay
  val TransactionsInBlock = 100

  val getRequiredData: GetDataFromCurrentView[NCBlockChain, NCState, NCWallet, NCCommandMemPool, RequiredForgingInfo] = {
    val f: CurrentView[NCBlockChain, NCState, NCWallet, NCCommandMemPool] => RequiredForgingInfo = {
      view: CurrentView[NCBlockChain, NCState, NCWallet, NCCommandMemPool] =>
        val toInclude = view.state.filterValid(view.pool.take(TransactionsInBlock).toSeq)
        val lastBlock = view.history.lastBlock.getOrElse(throw new Exception("Previous block not exist"))

        //Forger try to forge block for each key from vault. For NCWallet we have only one key :)
        val gbs: Seq[(PublicKey25519Proposition, Option[NCBox], PrivateKey25519)] = {
          view.vault.publicKeys.map { pk =>
            val boxOpt: Option[NCBox] = view.state.boxesOf(pk).headOption
            val secret: PrivateKey25519 = view.vault.secretByPublicImage(pk).get
            (pk, boxOpt, secret)
          }.toSeq
        }
        RequiredForgingInfo(toInclude, lastBlock, gbs)
    }
    GetDataFromCurrentView[NCBlockChain, NCState, NCWallet, NCCommandMemPool, RequiredForgingInfo](f)
  }
}