package net.shrine.adapter.dao.squeryl

import org.junit.{Ignore, Test}
import net.shrine.util.ShouldMatchersForJUnit
import org.squeryl.Query
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.adapter.dao.model.squeryl.SquerylPrivilegedUser
import net.shrine.dao.DateHelpers
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ResultOutputType.PATIENT_COUNT_XML
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.ResultOutputType
import net.shrine.dao.squeryl.SquerylEntryPoint

/**
 * @author clint
 * @since Nov 19, 2012
 *
 * Ported
 */
final class PrivilegedUsersTest extends AbstractSquerylAdapterTest with ShouldMatchersForJUnit {

  private val testDomain = "testDomain"
  private val testUsername = "testUsername"
  private val testAuthn = AuthenticationInfo(testDomain, testUsername, Credential("asdalksdasd", false))
  private val testThreshold = 3
  private val defaultThreshold = 10

  import SquerylEntryPoint._
  
  @Test
  def testGetUserThreshold = afterInsertingTestUser {
    val threshold = userThresholdQuery(testAuthn).single

    threshold should equal(testThreshold)
  }

  private def authn(domain: String, username: String) = AuthenticationInfo(domain, username, Credential("sakdjhajksdh", false))
  
  @Test
  def testIsUserWithNoThresholdEntryLockedOut = afterInsertingTestUser {
    val username = "noEntry"
    val domain = "noEntryDomain"

    val noThresholdId1 = authn(testDomain, username)
    val noThresholdId2 = authn(domain, testUsername)

    for (noThresholdId <- Seq(noThresholdId1, noThresholdId2)) {
      dao.isUserLockedOut(noThresholdId, defaultThreshold) should be(false)

      lockoutUser(noThresholdId, 42)

      dao.isUserLockedOut(noThresholdId, defaultThreshold) should be(false)
    }
  }

  @Test
  def testIsUserLockedOut = afterInsertingTestUser {
    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    logCountQueryResult("masterId:0", 0, testAuthn, 42)

    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    lockoutUser(testAuthn, 42)

    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(true)

    // Make sure username + domain is how users are identified
    dao.isUserLockedOut(authn(testDomain, "some-other-username"), defaultThreshold) should be(false)
    dao.isUserLockedOut(authn("some-other-domain", testUsername), defaultThreshold) should be(false)
  }

  @Test
  @Ignore
  def testIsUserNotLockedOutByRunningQueries = afterInsertingTestUser {
    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    logCountQueryResult("masterId:0", 0, testAuthn, 42)

    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    runQueriesDoNotLockoutUser(testAuthn, 42)

    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    // Make sure username + domain is how users are identified
    dao.isUserLockedOut(authn(testDomain, "some-other-username"), defaultThreshold) should be(false)
    dao.isUserLockedOut(authn("some-other-domain", testUsername), defaultThreshold) should be(false)
  }

  @Test
  def testIsUserLockedOutWithResultSetSizeOfZero = afterInsertingTestUser {
    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    logCountQueryResult("masterId:0", 0, testAuthn, 0)

    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    lockoutUser(testAuthn, 0)

    // user should not be locked out
    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)
  }

  @Test
  def testLockoutOverride = afterInsertingTestUser {
    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    lockoutUser(testAuthn, 42)

    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(true)

    val tomorrow = DateHelpers.daysFromNow(1)

    setOverrideDate(testAuthn, tomorrow)

    dao.isUserLockedOut(testAuthn, defaultThreshold) should be(false)

    val thirtyOneDaysAgo = DateHelpers.daysFromNow(-31)

    setOverrideDate(testAuthn, thirtyOneDaysAgo)
  }

  private def setOverrideDate(authn: AuthenticationInfo, newOverrideDate: XMLGregorianCalendar) {
    import DateHelpers.toTimestamp

    update(tables.privilegedUsers) { user =>
      where(user.username === authn.username and user.domain === authn.domain).set(user.overrideDate := Option(newOverrideDate).map(toTimestamp))
    }
  }
  
  private def lockoutUser(lockedOutAuthn: AuthenticationInfo, resultSetSize: Int) {
    for (i <- 1 until testThreshold + 2) {
      logCountQueryResult("masterId:" + i, i, lockedOutAuthn, resultSetSize)
    }
  }

  private def runQueriesDoNotLockoutUser(lockedOutAuthn: AuthenticationInfo, startResultSetSize: Int) {
    for (i <- 1 until testThreshold + 2) {
      logCountQueryResult("masterId:" + i, i, lockedOutAuthn, startResultSetSize + 1)
    }
  }

  private def logCountQueryResult(masterId: String, networkQueryId: Int, lockedOutAuthn: AuthenticationInfo, resultSetSize: Int) {
    val now = XmlDateHelper.now

    val expr = Term("blah")

    val queryDef = QueryDefinition("foo", expr)

    val insertedQueryId = dao.insertQuery(masterId, networkQueryId, lockedOutAuthn, queryDef, isFlagged = false, hasBeenRun = true, flagMessage = None)

    import ResultOutputType.PATIENT_COUNT_XML

    val queryResult = QueryResult(999, 123, Option(PATIENT_COUNT_XML), resultSetSize, Option(now), Option(now), None, QueryResult.StatusType.Finished, None)

    val insertedResultIds = dao.insertQueryResults(insertedQueryId, Seq(queryResult))

    dao.insertCountResult(insertedResultIds(PATIENT_COUNT_XML).head, resultSetSize, resultSetSize + 1)
    
    dao.findResultsFor(networkQueryId).get.count.data.get.originalValue should equal(resultSetSize)
  }
  
  private def userThresholdQuery(authn: AuthenticationInfo): Query[Int] = {
    from(tables.privilegedUsers) { user =>
      where(user.username === authn.username and user.domain === authn.domain).select(user.threshold)
    }
  }

  private def insertTestUser() {
    tables.privilegedUsers.insert(new SquerylPrivilegedUser(0, testUsername, testDomain, testThreshold, None: Option[XMLGregorianCalendar]))
  }

  private def afterInsertingTestUser(body: => Any): Unit = afterCreatingTables {
    insertTestUser()

    body
  }
}