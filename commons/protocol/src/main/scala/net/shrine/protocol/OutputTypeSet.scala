package net.shrine.protocol

/**
 *
 * @author Clint Gilbert
 * @date Sep 20, 2011
 *
 * @link http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * Wraps a Set[ResultOutputType], and provides a constructor that takes a String representing a
 * serialized Set[ResultOutputType].  Used by JAXRS to unmarshal params sent as Strings (@QueryParams,
 * @HeaderParams, etc.)
 *
 * NB: A case class for structural equality
 */
final case class OutputTypeSet(private val outputTypes: Set[ResultOutputType]) {
  require(outputTypes != null)

  def this(outputTypesString: String) = this(OutputTypeSet.deserialize(outputTypesString))

  import OutputTypeSet._

  def serialized: String = {
    require(outputTypes != null)

    encode(outputTypes.map(_.name).mkString(","))
  }

  def toSet = outputTypes
}

object OutputTypeSet {

  private[shrine] def deserialize(outputTypesString: String): Set[ResultOutputType] = {
    try {
      require(outputTypesString != null)
      
      if (outputTypesString == "") {
        Set.empty
      } else {
        //Intentionally use .get on the result of ResultOutputType.valueOf, to fail fast and preserve the 
        //contract of throwing a WebApplicationException
        decode(outputTypesString).split(",").map(r => ResultOutputType.valueOf(r).get).toSet
      }
    } catch {
      case e: Exception => throw new Exception(s"Couldn't parse '$outputTypesString' into a Set of ResultOutputTypes", e)
    }
  }

  private[this] val utf8 = "UTF-8"

  import java.net.{ URLEncoder, URLDecoder }

  private[shrine] def encode(s: String): String = URLEncoder.encode(s, utf8)

  private[shrine] def decode(s: String): String = URLDecoder.decode(s, utf8)
}