import java.time.Duration
import java.util.UUID

import org.apache.kafka.streams.processor.{
  Processor,
  ProcessorContext,
  PunctuationType
}
import org.apache.kafka.streams.state.KeyValueStore

import scala.jdk.CollectionConverters._

/*
 * https://docs.confluent.io/current/streams/developer-guide/processor-api.html#defining-a-stream-processor
 */

/*
 * TODO: Choosing of a useful r
 */

val r = 1

val a = 0.998

val lambda = 1

val steppingtime = 1

val decay = scala.math.pow(a, lambda * steppingtime)

class PointToClusterCellProcessor extends Processor[String, Point] {

  private var context: ProcessorContext = _
  private var clusterCells: KeyValueStore[String, ClusterCell] = _
  private var pointBuffer: KeyValueStore[String, Point] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
    clusterCells = context
      .getStateStore("ClusterCellStore")
      .asInstanceOf[KeyValueStore[String, ClusterCell]]
    pointBuffer = context
      .getStateStore("PointBuffer")
      .asInstanceOf[KeyValueStore[String, Point]]

    val duration = new Duration().withSeconds(1)

    context.schedule(
      duration,
      PunctuationType.WALL_CLOCK_TIME,
      _ => {
        val points = pointBuffer.all.asScala
        points.foreach(point => processPoint(point.key, point.value))
        context.commit()
      }
    )
  }

  def processPoint(key: String, value: Point): Unit = {
    val closestCell = clusterCells.all.asScala
      .map(cell => (cell, pointClusterCellDist(value, cell.value)))
      .minByOption(_._2)
      .filter(_._2 < r)
    if (closestCell.isEmpty) {
      // Create new cluster cell from point
      val newClusterCell = ClusterCell(
        value,
        1,
        clusterCells.all.asScala
          .filter(_.value.timelyDensity > 1)
          .map(cell => pointClusterCellDist(value, cell.value))
          .minByOption(dist => dist)
      )

      val newuuid = UUID.randomUUID.toString
      this.context.forward(newuuid, newClusterCell)
      this.clusterCells.put(newuuid, newClusterCell)
    } else {
      // merge found cluster cell with point
      val oldClusterCell = closestCell.get._1
      val timelyDensity = decay * oldClusterCell.value.timelyDensity + 1
      val seedPoint = oldClusterCell.value.seedPoint
      val mergedClusterCell = ClusterCell(
        seedPoint,
        timelyDensity,
        clusterCells.all.asScala
          .filter(
            cell => cell.key != oldClusterCell.key && cell.value.timelyDensity > timelyDensity
          )
          .map(cell => pointClusterCellDist(seedPoint, cell.value))
          .minByOption(dist => dist)
      )
      this.context.forward(oldClusterCell.key, mergedClusterCell)
      this.clusterCells.put(oldClusterCell.key, mergedClusterCell)
    }
  }

  def pointClusterCellDist(point: Point, clusterCell: ClusterCell): Double = {
    val x = point.x - clusterCell.seedPoint.x
    val y = point.y - clusterCell.seedPoint.y
    scala.math.sqrt((x * x) + (y * y))
  }

  override def process(key: String, value: Point): Unit = {
    pointBuffer.put(key, value)
  }

  override def close(): Unit = {}
}