package note.model

import com.google.common.primitives.{Booleans, Longs}
import io.circe.Json
import io.circe.syntax._
import scorex.core.serialization.{JsonSerializable, Serializer}
import scorex.core.transaction.account.PublicKeyNoncedBox
import scorex.core.transaction.box.Box.Amount
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.crypto.encode.{Base16, Base58}
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.{Curve25519, PublicKey}

import scala.util.Try

/**
  * NCBox Case Class
  *
  * @param proposition
  * @param nonce
  * @param isForgerBox
  * @param title
  * @param tasks
  */
case class NCBox(override val proposition: PublicKey25519Proposition,
                 override val nonce: Nonce,
                 isForgerBox: Boolean,
                 title: String = "",
                 tasks: Seq[String] = Seq())
           extends PublicKeyNoncedBox[PublicKey25519Proposition]
           with JsonSerializable {

  override def json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "address" -> proposition.address.asJson,
    "publicKey" -> Base58.encode(proposition.pubKeyBytes).asJson,
    "nonce" -> nonce.toLong.asJson,
    "value" -> value.toLong.asJson,
    "title" -> title.asJson,
    "notes" -> tasks.asJson
  ).asJson

  override type M = NCBox

  override def serializer: Serializer[NCBox] = Offer25519BoxSerializer

  override def toString: String =
    s"NCBox(id: ${Base16.encode(id)}, proposition: $proposition, nonce: $nonce, value: $value)"

  override val value: Amount = 0
}

/**
  * NCBox Companion Object
  */
object NCBox {
  val BoxKeyLength = Blake2b256.DigestSize
  val BoxLength: Int = Curve25519.KeyLength + 2 * 8
}

/**
  * Offer25519BoxSerializer Object
  */
object Offer25519BoxSerializer extends Serializer[NCBox] {

  override def toBytes(obj: NCBox): Array[Byte] =
    obj.proposition.pubKeyBytes ++
      Longs.toByteArray(obj.nonce) ++
      Longs.toByteArray(obj.value) ++
      Longs.toByteArray(obj.value)

  override def parseBytes(bytes: Array[Byte]): Try[NCBox] = Try {
    val pk = PublicKey25519Proposition(PublicKey @@ bytes.take(32))
    val nonce = Nonce @@ Longs.fromByteArray(bytes.slice(32, 40))
    val value = Value @@ Longs.fromByteArray(bytes.slice(40, 48))
    val isForgerBox: Boolean = bytes.slice(40, 48).head.isValidByte
    NCBox(pk, nonce, isForgerBox)
  }
}
