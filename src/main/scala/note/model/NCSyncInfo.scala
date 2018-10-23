package note.model

import scorex.core.consensus.{BlockChain, SyncInfo}
import scorex.core.network.message.SyncInfoMessageSpec
import scorex.core.serialization.Serializer
import scorex.core.{ModifierId, ModifierTypeId, NodeViewModifier}

import scala.util.Try

/**
  * NCSyncInfo Case Class
  *
  * @param answer
  * @param lastBlockID
  * @param score
  */
case class NCSyncInfo(answer: Boolean, lastBlockID: ModifierId, score: BlockChain.Score) extends SyncInfo {

  override def startingPoints: Seq[(ModifierTypeId, ModifierId)] =
    Seq(NCBlock.ModifierTypeId -> lastBlockID)

  override type M = NCSyncInfo
  override def serializer: Serializer[NCSyncInfo] = NCSyncInfoSerializer
}

/**
  * NCSyncInfoSerializer Object
  */
object NCSyncInfoSerializer extends Serializer[NCSyncInfo] {

  override def toBytes(obj: NCSyncInfo): Array[Byte] =
    (if (obj.answer) 1: Byte else 0: Byte) +: (obj.lastBlockID ++ obj.score.toByteArray)

  def parseBytes(bytes: Array[Byte]): Try[NCSyncInfo] = Try {
    val answer = if (bytes.head == 1) true else if (bytes.head == 0) false else throw new Exception("wrong answer byte")
    val mid = ModifierId @@ bytes.tail.take(NodeViewModifier.ModifierIdSize)
    val scoreBytes = bytes.tail.drop(NodeViewModifier.ModifierIdSize)
    NCSyncInfo(answer, mid, BigInt(scoreBytes))
  }
}

/**
  * NCSyncInfoMessageSpec Object
  */
object NCSyncInfoMessageSpec extends SyncInfoMessageSpec[NCSyncInfo](NCSyncInfoSerializer.parseBytes)