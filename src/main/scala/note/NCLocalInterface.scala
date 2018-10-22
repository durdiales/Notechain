package note

import akka.actor.{ActorRef, ActorSystem, Props}
import note.mining.{MiningSettings, NCForger}
import note.model.{NCBlock, BaseEvent}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.{LocalInterface, ModifierId}

/**
  * NCLocalInterface Class
  *
  * @param viewHolderRef
  * @param forgerRef
  * @param minerSettings
  */
class NCLocalInterface(override val viewHolderRef: ActorRef,
                       forgerRef: ActorRef,
                       minerSettings: MiningSettings)
      extends LocalInterface[PublicKey25519Proposition, BaseEvent, NCBlock] {

  private var block = false

  override protected def onStartingPersistentModifierApplication(pmod: NCBlock): Unit = {}

  override protected def onFailedTransaction(tx: BaseEvent): Unit = {}

  override protected def onSyntacticallyFailedModification(mod: NCBlock): Unit = {}

  override protected def onSuccessfulTransaction(tx: BaseEvent): Unit = {}

  override protected def onSyntacticallySuccessfulModification(mod: NCBlock): Unit = {}

  override protected def onSemanticallyFailedModification(mod: NCBlock): Unit = {}

  override protected def onNewSurface(newSurface: Seq[ModifierId]): Unit = {}

  override protected def onRollbackFailed(): Unit = {log.error("Too deep rollback occurred!")}

  override protected def onSemanticallySuccessfulModification(mod: NCBlock): Unit = {}

  override protected def onNoBetterNeighbour(): Unit = forgerRef ! NCForger.StartMining

  override protected def onBetterNeighbourAppeared(): Unit = forgerRef ! NCForger.StopMining
}

/**
  * NCLocalInterfaceRef Object
  */
object NCLocalInterfaceRef {
  def props(viewHolderRef: ActorRef,
            minerRef: ActorRef,
            minerSettings: MiningSettings): Props = {
    Props(new NCLocalInterface(viewHolderRef, minerRef, minerSettings))
  }
  def apply(viewHolderRef: ActorRef,
            minerRef: ActorRef,
            minerSettings: MiningSettings)
           (implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(viewHolderRef, minerRef, minerSettings))
  }
  def apply(name: String, viewHolderRef: ActorRef,
            minerRef: ActorRef,
            minerSettings: MiningSettings)
           (implicit system: ActorSystem): ActorRef = {

    system.actorOf(props(viewHolderRef, minerRef, minerSettings), name)
  }
}
