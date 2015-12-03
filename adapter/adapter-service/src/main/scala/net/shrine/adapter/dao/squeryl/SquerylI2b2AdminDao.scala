package net.shrine.adapter.dao.squeryl

import org.squeryl.Query
import org.squeryl.dsl.ast.BinaryOperatorNodeLogicalBoolean
import org.squeryl.dsl.ast.OrderByArg
import net.shrine.adapter.dao.I2b2AdminDao
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.model.squeryl.SquerylShrineQuery
import net.shrine.adapter.dao.squeryl.tables.Tables
import net.shrine.dao.squeryl.SquerylInitializer
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest.SortOrder
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest.Strategy
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.I2b2AdminUserWithRole
import net.shrine.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @date Apr 24, 2013
 */
final class SquerylI2b2AdminDao(shrineProjectId: String, initializer: SquerylInitializer, val tables: Tables) extends I2b2AdminDao {
  initializer.init()

  import SquerylEntryPoint._

  import ReadI2b2AdminPreviousQueriesRequest.{SortOrder, Category, Username}
  
  override def findQueriesByUserFlaggedStatusDateAndSearchString(username: Username, category: Category, searchString: String, howMany: Int, startDate: Option[XMLGregorianCalendar], strategy: Strategy, sortOrder: SortOrder): Seq[ShrineQuery] = {
    inTransaction {
      Queries.previousQueriesByUserFlaggedStatusAndSearchString(username, category, searchString, startDate, sortOrder, strategy).take(howMany).toSeq
    }
  }
  
  override def findQueryingUsersByProjectId(projectId: String): Seq[I2b2AdminUserWithRole] = {
    inTransaction {
      Queries.queryingUsersByProjectId.toIndexedSeq
    }
  }

  private object Queries {
    val queryingUsersByProjectId: Query[I2b2AdminUserWithRole] = {
      //NB: Shrine queries are always run under the same project id, as defined in shrine.conf; use this value
      //when returning I2b2AdminUserWithRoles.  Additionally, since Shrine doesn't have a concept of roles, just
      //return 'USER' for the role.
      def toI2b2AdminUserWithRole(query: SquerylShrineQuery) = I2b2AdminUserWithRole(shrineProjectId, query.username, "USER")
      
      from(tables.shrineQueries) { query =>
        select(toI2b2AdminUserWithRole(query))/*.
        orderBy(query.dateCreated)*/ //TODO: XXX: ???
      }.distinct
    }
    
    import ReadI2b2AdminPreviousQueriesRequest.{SortOrder, Strategy, Username}
    
    private def nameMatchFunctionFor(strategy: ReadI2b2AdminPreviousQueriesRequest.Strategy): (String, String) => BinaryOperatorNodeLogicalBoolean = {
      import ReadI2b2AdminPreviousQueriesRequest.Strategy._

      strategy match {
        case Contains => (lhs, rhs) => lhs like s"%$rhs%"
        case Exact => _ === _
        case Left => (lhs, rhs) => lhs like s"$rhs%"
        case Right => (lhs, rhs) => lhs like s"%$rhs"
      }
    }

    private def dateMatchFunctionFor(sortOrder: ReadI2b2AdminPreviousQueriesRequest.SortOrder): (SquerylShrineQuery, XMLGregorianCalendar) => BinaryOperatorNodeLogicalBoolean = {
      import ReadI2b2AdminPreviousQueriesRequest.SortOrder._

      sortOrder match {
        case Ascending => (query, cutoffDate) => query.dateCreated > toTimestamp(cutoffDate)
        case Descending => (query, cutoffDate) => query.dateCreated < toTimestamp(cutoffDate)
      }
    }
    
    private def sortFunctionFor(sortOrder: ReadI2b2AdminPreviousQueriesRequest.SortOrder): SquerylShrineQuery => OrderByArg = {
      import ReadI2b2AdminPreviousQueriesRequest.SortOrder._

      sortOrder match {
        case Ascending => _.dateCreated.asc
        case Descending => _.dateCreated.desc
      }
    }

    //NB: Will use current locale; this might be a problem, or might not
    private def toTimestamp(xmlGc: XMLGregorianCalendar) = new java.sql.Timestamp(xmlGc.toGregorianCalendar.getTime.getTime)

    def previousQueriesByUserFlaggedStatusAndSearchString(
      username: Username,
      category: Category,
      searchString: String,
      startDateOption: Option[XMLGregorianCalendar],
      sortOrder: SortOrder,
      nameMatchStrategy: Strategy): Query[ShrineQuery] = {

      val nameMatches: (String, String) => BinaryOperatorNodeLogicalBoolean = nameMatchFunctionFor(nameMatchStrategy)
      
      val dateMatches: (SquerylShrineQuery, XMLGregorianCalendar) => BinaryOperatorNodeLogicalBoolean = dateMatchFunctionFor(sortOrder)
      
      val ordering: SquerylShrineQuery => OrderByArg = sortFunctionFor(sortOrder)
      
      //def toUsernameClause(u: )
      
      from(tables.shrineQueries) { query =>
        where({
          val nameAndUsernamePredicate = {
            val nameMatchPredicate = nameMatches(query.name, searchString)

            if (username.value != ReadI2b2AdminPreviousQueriesRequest.allUsers) {
              val usernameOp: (String, String) => BinaryOperatorNodeLogicalBoolean = if(username.isExact) { _ === _ } else { _ <> _ }
              
              usernameOp(query.username, username.value) and nameMatchPredicate
            } 
            else { nameMatchPredicate }
          }

          val withStartDatePredicate = startDateOption match {
            case Some(startDate) => nameAndUsernamePredicate and dateMatches(query, startDate)
            case None => nameAndUsernamePredicate
          }
          
          if(category.isFlagged) { withStartDatePredicate and query.isFlagged === true }
          else { withStartDatePredicate }
        }).
        select(query.toShrineQuery).
        orderBy(ordering(query))
      }
    }
  }
}