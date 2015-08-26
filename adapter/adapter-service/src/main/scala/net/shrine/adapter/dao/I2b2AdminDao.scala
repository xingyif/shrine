package net.shrine.adapter.dao

import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.I2b2AdminUserWithRole

/**
 * @author clint
 * @date Apr 24, 2013
 */
trait I2b2AdminDao {
  import ReadI2b2AdminPreviousQueriesRequest.{ SortOrder, Strategy, Username, Category }

  def findQueriesByUserFlaggedStatusDateAndSearchString(username: Username, category: Category, searchString: String, howMany: Int, startDate: Option[XMLGregorianCalendar], strategy: Strategy, sortOrder: SortOrder): Seq[ShrineQuery]
  
  def findQueryingUsersByProjectId(projectId: String): Seq[I2b2AdminUserWithRole]
}
