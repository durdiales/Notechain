package note.model

import com.google.common.primitives.{Ints, Longs}
import io.circe.Json
import io.circe.syntax._
import scorex.core.block.Block
import scorex.core.block.Block.{BlockId, Version}
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.{ModifierId, ModifierTypeId, PersistentNodeViewModifier}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256

import scala.util.Try

/**
  * NCBlock Case Class
  *
  * @param parentId
  * @param timestamp
  * @param generationSignature
  * @param baseTarget
  * @param generator
  * @param transactions
  */
case class NCBlock(override val parentId: BlockId,
                   override val timestamp: Block.Timestamp,
                   generationSignature: GenerationSignature,
                   baseTarget: BaseTarget,
                   generator: PublicKey25519Proposition,
                   override val transactions: Seq[BaseEvent])
           extends PersistentNodeViewModifier
           with Block[PublicKey25519Proposition, BaseEvent] {

  override lazy val version: Version = 0: Byte

  override def json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "parentId" -> Base58.encode(parentId).asJson,
    "timestamp" -> timestamp.asJson,
    "events" -> transactions.map(_.json).asJson,
    "generator" -> generator.address.asJson
  ).asJson

  override val modifierTypeId: ModifierTypeId = NCBlock.ModifierTypeId
  override def id: ModifierId = ModifierId @@ Blake2b256(serializer.messageToSign(this))
  override type M = NCBlock
  override lazy val serializer = NCBlockSerializer
  override def toString: String = s"NCBlock(${json.noSpaces})"
}

/**
  * NCBlockSerializer Object
  */
object NCBlockSerializer extends Serializer[NCBlock] {

  def messageToSign(block: NCBlock): Array[Byte] = {
    block.parentId ++
      Longs.toByteArray(block.timestamp) ++
      Array(block.version) ++
      Longs.toByteArray(block.baseTarget) ++
      block.generator.pubKeyBytes ++ {
        val cntBytes = Ints.toByteArray(block.transactions.size)
        block.transactions.foldLeft(cntBytes) { case (bytes, tx) => bytes ++ tx.bytes }
      }
  }

  override def toBytes(obj: NCBlock): Array[Version] = ???
  override def parseBytes(bytes: Array[Version]): Try[NCBlock] = ???
}

/**
  * Companion Object NCBlock
  */
object NCBlock {
  val ModifierTypeId: ModifierTypeId = scorex.core.ModifierTypeId @@ 1.toByte
  val SignatureLength = 64
}