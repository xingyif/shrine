package net.shrine.adapter.dao.model

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.QueryMaster

/**
 * @author clint
 * @since Oct 16, 2012
 * 
 * NB: Can't be final, since Squeryl runs this class through cglib to make a synthetic subclass :(
 */
final case class ShrineQuery(
  id: Int,
  localId: String,
  networkId: Long,
  name: String,
  username: String,
  domain: String,
  dateCreated: XMLGregorianCalendar,
  isFlagged: Boolean,
  hasBeenRun: Boolean, //todo this goes next
  flagMessage: Option[String],
  queryDefinition: QueryDefinition) {
  
  def hasNotBeenRun: Boolean = !hasBeenRun

  def withName(newName: String): ShrineQuery = this.copy(name = newName)

  //NB: Due to the new i2b2 admin previous queries API, we need to be able to transform
  //ourselves into a QueryMaster using either the network or local id .
  def toQueryMaster(idField: ShrineQuery => String = _.networkId.toString): QueryMaster = {
    QueryMaster(idField(this), networkId, name, username, domain, dateCreated, Some(isFlagged), if(isFlagged) flagMessage else None)
  }
}
