package net.shrine.adapter.service

import net.shrine.adapter.AdapterTestHelpers
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential

/**
 * @author clint
 * @date Apr 23, 2013
 */
trait CanLoadTestData { self: AbstractSquerylAdapterTest with AdapterTestHelpers =>
  private var _insertedQueryId1: Int = _
  private var _insertedQueryId2: Int = _
  private var _insertedQueryId3: Int = _
  private var _insertedQueryId4: Int = _

  def insertedQueryId1: Int = _insertedQueryId1
  def insertedQueryId2: Int = _insertedQueryId2
  def insertedQueryId3: Int = _insertedQueryId3
  def insertedQueryId4: Int = _insertedQueryId4

  protected def loadTestData() {
    _insertedQueryId1 = dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = true, hasBeenRun = true, flagMessage = None)

    //NB: Sleep for a few ms to make sure queries are stored with timestamps that differ.
    //java.sql.Date values have millisecond-level resolution.
    Thread.sleep(5)

    _insertedQueryId2 = dao.insertQuery(masterId2, networkQueryId2, authn, queryDef2, isFlagged = false, hasBeenRun = true, flagMessage = None)

    Thread.sleep(5)

    _insertedQueryId3 = dao.insertQuery(masterId3, networkQueryId3, authn2, queryDef2, isFlagged = false, hasBeenRun = true, flagMessage = None)

    Thread.sleep(5)

    _insertedQueryId4 = dao.insertQuery(masterId4, networkQueryId4, authn2.copy(domain = anotherDomain), queryDef2, isFlagged = true, hasBeenRun = true, flagMessage = None)
  }

  protected def afterLoadingTestData(f: => Any): Unit = afterCreatingTables {
    try {
      loadTestData()
    } finally {
      f
    }
  }
}