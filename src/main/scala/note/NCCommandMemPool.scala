package note

import note.model.BaseEvent
import io.iohk.iodb.ByteArrayWrapper
import scorex.core.ModifierId
import scorex.core.transaction.MemoryPool
import scorex.core.utils.ScorexLogging

import scala.collection.concurrent.TrieMap
import scala.util.{Success, Try}

/**
  * NCCommandMemPool Case Class
  *
  * @param unconfirmed
  */
case class NCCommandMemPool(unconfirmed: TrieMap[ByteArrayWrapper, BaseEvent])
           extends MemoryPool[BaseEvent, NCCommandMemPool]
           with ScorexLogging {

  override type NVCT = NCCommandMemPool

  private def key(id: Array[Byte]): ByteArrayWrapper = ByteArrayWrapper(id)

  //getters
  override def getById(id: ModifierId): Option[BaseEvent] = unconfirmed.get(key(id))

  override def contains(id: ModifierId): Boolean = unconfirmed.contains(key(id))

  override def getAll(ids: Seq[ModifierId]): Seq[BaseEvent] = ids.flatMap(getById)

  //modifiers
  override def put(tx: BaseEvent): Try[NCCommandMemPool] = Success {
    unconfirmed.put(key(tx.id), tx)
    this
  }

  override def put(txs: Iterable[BaseEvent]): Try[NCCommandMemPool] = Success(putWithoutCheck(txs))

  override def putWithoutCheck(txs: Iterable[BaseEvent]): NCCommandMemPool = {
    txs.foreach(tx => unconfirmed.put(key(tx.id), tx))
    this
  }

  override def remove(tx: BaseEvent): NCCommandMemPool = {
    unconfirmed.remove(key(tx.id))
    this
  }

  override def take(limit: Int): Iterable[BaseEvent] =
    unconfirmed.values.toSeq.sortBy(-_.fee).take(limit)

  override def filter(condition: (BaseEvent) => Boolean): NCCommandMemPool = {
    unconfirmed.retain { (k, v) =>
      condition(v)
    }
    this
  }

  override def size: Int = unconfirmed.size
}

/**
  * Companion Object
  */
object NCCommandMemPool {
  lazy val emptyPool: NCCommandMemPool = NCCommandMemPool(TrieMap())
}