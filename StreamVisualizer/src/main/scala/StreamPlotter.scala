import java.awt.BorderLayout
import java.time.Duration
import java.util.Properties
import java.util.regex.Pattern

import javax.swing.{JFrame, JPanel}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle
import org.knowm.xchart.{XChartPanel, XYChart, XYChartBuilder}
import types.cell.{ClusterCell, ClusterCellDeserializer}
import types.point.{Point, PointDeserializer}

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.util.control.Breaks._

object StreamPlotter extends App {

  def makeChart = {
    val chart = new XYChartBuilder()
      .width(600)
      .height(500)
      .title("Stream Plot")
      .xAxisTitle("X")
      .yAxisTitle("Y")
      .build

    chart.getStyler.setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter)

    chart.addSeries("Points", new Array[Double](1))
    chart.addSeries("Clusters", new Array[Double](1))

    chart
  }

  val properties = new Properties()
  properties.put("bootstrap.servers", "localhost:9092")
  properties.put("group.id", "stream-generator")

  val clusterConsumer =
    new KafkaConsumer[String, ClusterCell](
      properties,
      new StringDeserializer,
      new ClusterCellDeserializer
    )
  val pointConsumer =
    new KafkaConsumer[String, Point](
      properties,
      new StringDeserializer,
      new PointDeserializer
    )

  val chart = makeChart
  var chartPanel: JPanel = _

  javax.swing.SwingUtilities.invokeLater(() => {
    val frame = new JFrame("Advanced Example")
    frame.setLayout(new BorderLayout)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    chartPanel = new XChartPanel[XYChart](chart)
    frame.add(chartPanel, BorderLayout.CENTER)

    frame.pack()
    frame.setVisible(true)
  })

  clusterConsumer.subscribe(Pattern.compile("streams-clusters-input"))
  pointConsumer.subscribe(Pattern.compile("streams-points-input"))

  val ringbuffer = mutable.Queue[Point]()
  val clustersFinal = mutable.Map[String, Point]()

  while (true) breakable {
    val pointResult = pointConsumer.poll(Duration.ofSeconds(1))
    val clusterResult = clusterConsumer.poll(Duration.ofSeconds(1))

    if (pointResult.count() == 0 && clusterResult.count() == 0) break

    val points: Iterable[Point] = pointResult.asScala.map(_.value())

    points.foreach(point => {
      if (ringbuffer.length >= 500) ringbuffer.dequeue()
      ringbuffer += point
    })

    clusterResult.asScala.foreach(
      cell => clustersFinal.put(cell.key(), cell.value().seedPoint)
    )

    val xvalsPoints = ringbuffer.map(_.x).toArray
    val yvalsPoints = ringbuffer.map(_.y).toArray

    val xvalsClusters = clustersFinal.values.map(_.x).toArray
    val yvalsClusters = clustersFinal.values.map(_.y).toArray

    javax.swing.SwingUtilities.invokeLater(() => {
      chart.updateXYSeries("Points", xvalsPoints, yvalsPoints, null)
      chart.updateXYSeries("Clusters", xvalsClusters, yvalsClusters, null)
      chartPanel.repaint()
    })
  }
}