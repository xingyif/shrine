package net.shrine.adapter.dao.model.squeryl

import net.shrine.adapter.dao.model.ShrineError
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

import scala.xml.XML

/**
 * @author clint
 * @since May 28, 2013
 */
case class SquerylShrineError(
    @Column(name = "ID")
    id: Int, 
    @Column(name = "RESULT_ID")
    resultId: Int, 
    @Column(name = "MESSAGE")
    message: String,
    @Column(name = "CODEC")
    codec:String,
    @Column(name = "stamp")
    stamp:String,
    @Column(name = "SUMMARY")
    summary:String,
    @Column(name = "PROBLEM_DESCRIPTION")
    digestDescription:String,
    @Column(name = "DETAILS")
    details:String
  ) extends KeyedEntity[Int] {
  
  //NB: For Squeryl, ugh :(
  def this() = this(0, 0, "", "", "", "", "", "")
  
  def toShrineError = {
      val detailsXml = if(""!=details) XML.loadString(details)
                        else <details/>

      ShrineError(id, resultId, message, codec, stamp, summary, digestDescription, detailsXml)
  }
}
