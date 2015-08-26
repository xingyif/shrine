package net.shrine.adapter

import xml.NodeSeq
import net.shrine.protocol._
import net.shrine.client.HttpClient
import net.shrine.client.Poster

/**
 * @author ??? (Dave Ortiz? Justin Quan?)
 * @date ???
 */
final class ReadPdoAdapter(
    poster: Poster,
    override protected val hiveCredentials: HiveCredentials)
    extends CrcAdapter[ReadPdoRequest, ReadPdoResponse](poster, hiveCredentials) {

  override protected def parseShrineResponse(nodeSeq: NodeSeq) = ReadPdoResponse.fromI2b2(nodeSeq)
}
