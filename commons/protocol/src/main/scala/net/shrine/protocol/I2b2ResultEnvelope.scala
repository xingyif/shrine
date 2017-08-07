package net.shrine.protocol

import net.liftweb.json.JsonDSL._
import net.liftweb.json._
import net.shrine.serialization.{I2b2Marshaller, JsonMarshaller, XmlMarshaller}
import net.shrine.util.{NodeSeqEnrichments, XmlUtil}

import scala.util.{Success, Try}
import scala.xml.NodeSeq


/**
 * @author clint
 * @since Aug 15, 2012
 */
final case class I2b2ResultEnvelope(resultType: ResultOutputType, data: Map[String, Long]) extends I2b2Marshaller with XmlMarshaller with JsonMarshaller {
  import I2b2ResultEnvelope._
  
  //Extra parameter list with dummy int value needed to disambiguate this constructor and the class-level one, which without
  //the extra param list have the same signature after erasure. :/  Making the dummy param implicit lets us omit the second 
  //param list entirely when calling this constructor.
  def this(resultType: ResultOutputType, cols: (String, Long)*) = this(resultType, cols.toMap)

  def +(column: ColumnTuple): I2b2ResultEnvelope = {
    this.copy(data = data + column)
  }

  def ++(columns: Iterable[ColumnTuple]): I2b2ResultEnvelope = {
    columns.foldLeft(this)(_ + _)
  }

  def mapValues(f: Long => Long): I2b2ResultEnvelope = this.copy(data = data.mapValues(f))
  
  def toMap: Map[String, Long] = data

  private def sortedData: Seq[(String, Long)] = data.toSeq.sortBy { case (columnName, _) => columnName }
  
  override def toI2b2: NodeSeq = XmlUtil.stripWhitespace {
    <ns10:i2b2_result_envelope xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns9="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns10="http://www.i2b2.org/xsd/hive/msg/result/1.1/" xmlns:ns7="http://www.i2b2.org/xsd/cell/crc/psm/analysisdefinition/1.1/" xmlns:ns8="http://www.i2b2.org/xsd/cell/pm/1.1/">
      <body>
        <ns10:result name={ resultType.name }>
          {
            //NB: SHRINE-863: Bill wants these sorted by name, server-side
            sortedData.map { case (name, value) => <data type="int" column={ name }>{ value }</data> }
          }
        </ns10:result>
      </body>
    </ns10:i2b2_result_envelope>
  }

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <resultEnvelope>
      <resultType>{ resultType }</resultType>
      { 
        //NB: SHRINE-863: Bill wants these sorted by name, server-side
        sortedData.map { case (name, value) =>
          <column>
            <name>{ name }</name>
            <value>{ value }</value>
          </column>
        }
      }
    </resultEnvelope>
  }
    
  override def toJson: JValue = resultType.name -> data
}

object I2b2ResultEnvelope extends I2b2XmlUnmarshaller[I2b2ResultEnvelope] with ShrineXmlUnmarshaller[I2b2ResultEnvelope] {

  type ColumnTuple = (String, Long)
  
  private object Column {
    
    private def unmarshal(xml: NodeSeq, name: NodeSeq => String, value: NodeSeq => Long): Try[ColumnTuple] = {
      Try {
        (name(xml), value(xml))
      }
    }
    
    private def from(attr: String): NodeSeq => String = xml => (xml \ attr).text
    
    def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ColumnTuple] = unmarshal(xml, from("@column"), _.text.toLong)
    
    def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ColumnTuple] = unmarshal(xml, from("name"), from("value") andThen (_.toLong))
    
    def fromJson(json: JValue): Option[ColumnTuple] = json match {
      case JObject(List(JField(name, JInt(value)))) => Some((name, value.toLong))
      case _ => None
    }
  }

  def empty(resultType: ResultOutputType) = new I2b2ResultEnvelope(resultType)

  import NodeSeqEnrichments.Strictness._
  
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[I2b2ResultEnvelope] = {
    unmarshal(
        Success(xml),
        _.withChild("resultType").map(_.text.trim).flatMap(ResultOutputType.tryValueOf(breakdownTypes)),
        _.withChild("column"), 
        Column.fromXml(breakdownTypes))
  }
  
  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(i2b2Xml: NodeSeq): Try[I2b2ResultEnvelope] = {
    unmarshal(
        i2b2Xml.withChild("body").withChild("result"), 
        _.attribute("name").flatMap(ResultOutputType.tryValueOf(breakdownTypes)), 
        x => Try(x \ "data"),
        Column.fromI2b2(breakdownTypes))
  }
  
  private def unmarshal(xmlAttempt: Try[NodeSeq], getResultType: NodeSeq => Try[ResultOutputType], columnXmls: NodeSeq => Try[NodeSeq], toColumn: NodeSeq => Try[ColumnTuple]): Try[I2b2ResultEnvelope] = {
    import net.shrine.util.Tries.sequence
    
    for {
      xml <- xmlAttempt
      resultType <- getResultType(xml)
      columnXml <- columnXmls(xml)
      columnAttempts = columnXml.map(toColumn)
      columns <- sequence(columnAttempts)
    } yield new I2b2ResultEnvelope(resultType, columns: _*)
  }
}