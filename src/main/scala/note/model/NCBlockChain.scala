package note.model

import note.model.NCBlockChain.Height
import scorex.core.consensus.History.{HistoryComparisonResult, ProgressInfo}
import scorex.core.consensus.{BlockChain, ModifierSemanticValidity}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.crypto.encode.Base58

import scala.util.{Failure, Success, Try}

/**
  * NCBlockChain Case Class
  *
  * @param blockIds
  * @param blocks
  */
case class NCBlockChain(blockIds: Map[Height, ModifierId] = Map(), blocks: Map[ModifierId, NCBlock] = Map())
  extends BlockChain[PublicKey25519Proposition, BaseEvent, NCBlock, NCSyncInfo, NCBlockChain] {

  import BlockChain.Score

  /**
    * If there's no history, even genesis block
    *
    * @return
    */
  override def isEmpty: Boolean = blocks.isEmpty

  override def modifierById(blockId: ModifierId): Option[NCBlock] =
    blocks.find(_._1.sameElements(blockId)).map(_._2)

  override def append(block: NCBlock): Try[(NCBlockChain, ProgressInfo[NCBlock])] = synchronized {
    log.debug(s"Trying to append block ${Base58.encode(block.id)} to history")
    val blockId = block.id
    val parentId = block.parentId

    if (blockIds.isEmpty || (lastBlock.getOrElse(throw new Exception("XXX")).id sameElements parentId)) { //TODO FIXME
      val h = height() + 1
      val newChain = NCBlockChain(blockIds + (h -> blockId), blocks + (blockId -> block))
      Success(newChain, ProgressInfo(None, Seq(), Some(block), Seq()))
    } else {
      val e = new Exception(s"Last block id is ${Base58.encode(blockIds.last._2)}, expected ${Base58.encode(parentId)}}")
      Failure(e)
    }
  }

  override def openSurfaceIds(): Seq[ModifierId] = Seq(blockIds(height()))

  override def continuationIds(info: NCSyncInfo,
                               size: Int): Option[Seq[(ModifierTypeId, ModifierId)]] = {
    val from = info.startingPoints
    require(from.size == 1)
    require(from.head._1 == NCBlock.ModifierTypeId)

    val fromId = from.head._2

    blockIds.find(_._2 sameElements fromId).map { case (fromHeight, _) =>
      (fromHeight + 1).to(fromHeight + size)
        .flatMap(blockIds.get)
        .map(id => NCBlock.ModifierTypeId -> id)
    }
  }

  /**
    * Quality score of a best chain, e.g. cumulative difficulty in case of Bitcoin / Nxt
    *
    * @return
    */
  override def chainScore(): BigInt = blocks.map(ib => score(ib._2)).sum

  override type NVCT = NCBlockChain

  override def score(block: NCBlock): Score = BigInt("18446744073709551616") / block.baseTarget

  /**
    * Height of the a chain, or a longest chain in an explicit block-tree
    */
  override def height(): Height = blocks.size

  override def heightOf(blockId: ModifierId): Option[Height] =
    blockIds.find(_._2 sameElements blockId).map(_._1)

  override def blockAt(height: Height): Option[NCBlock] =
    blockIds.get(height).flatMap(blocks.get)

  override def children(blockId: ModifierId): Seq[NCBlock] =
    heightOf(blockId).map(_ + 1).flatMap(blockAt).toSeq

  override def syncInfo: NCSyncInfo =
    NCSyncInfo(false, lastBlock.getOrElse(throw new Exception("UU")).id, chainScore())

  override def compare(other: NCSyncInfo): HistoryComparisonResult.Value = {
    val local = syncInfo.score
    val remote = other.score
    if (local < remote) HistoryComparisonResult.Older
    else if (local == remote) HistoryComparisonResult.Equal
    else HistoryComparisonResult.Younger
  }

  override def reportSemanticValidity(modifier: NCBlock,
                                      valid: Boolean,
                                      lastApplied: ModifierId): (NCBlockChain, ProgressInfo[NCBlock]) = {
    this -> ProgressInfo(None, Seq(), None, Seq())
  }

  override def isSemanticallyValid(modifierId: ModifierId): ModifierSemanticValidity.Value = ???
}

/**
  * Companion Object NCBlockChain
  */
object NCBlockChain {
  type Height = Int
}
