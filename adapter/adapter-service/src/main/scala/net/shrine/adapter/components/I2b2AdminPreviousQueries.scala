package net.shrine.adapter.components

import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.adapter.dao.I2b2AdminDao

/**
 * @author clint
 * @date Apr 4, 2013
 */
final case class I2b2AdminPreviousQueries(i2b2AdminDao: I2b2AdminDao) {
  def get(request: ReadI2b2AdminPreviousQueriesRequest): ShrineResponse = {
    val queries = i2b2AdminDao.findQueriesByUserFlaggedStatusDateAndSearchString(request.username, request.categoryToSearchWithin, request.searchString, request.maxResults, request.startDate, request.searchStrategy, request.sortOrder)

    ReadPreviousQueriesResponse(queries.map(_.toQueryMaster(_.localId)))
  }
}