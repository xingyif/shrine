package net.shrine.utilities.adapterqueriestoqep

import java.io.File
import javax.sql.DataSource
import javax.xml.datatype.XMLGregorianCalendar

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.squeryl.SquerylAdapterDao
import net.shrine.audit.NetworkQueryId
import net.shrine.dao.squeryl.{DataSourceSquerylInitializer, SquerylInitializer, SquerylDbAdapterSelecter}
import net.shrine.adapter.dao.squeryl.tables.{Tables => AdapterTables}
import net.shrine.protocol.ResultOutputTypes
import net.shrine.qep.QepConfigSource
import net.shrine.qep.queries.{QepQueryDb, QepQuery, QepQueryFlag}
import net.shrine.slick.TestableDataSourceCreator
import org.squeryl.internals.DatabaseAdapter
import net.shrine.config.ConfigExtensions

/**
 * @author dwalend
 * @since 1.21
 */

object AdapterQueriesToQep {
  def main(args: Array[String]): Unit = {
    if(args.length < 3) throw new IllegalArgumentException("Requires three arguments: the domain to transfer, the full path to the adapter-queries-to-qep.conf file, and the full path to the shrine.conf file.")

    val domain = args(0)
    val localConfig = args(1)
    val shrineConfig = args(2)

    val config: Config = ConfigFactory.parseFile(new File(localConfig)).withFallback(ConfigFactory.parseFile(new File(shrineConfig))).withFallback(ConfigFactory.load())

    val adapterDataSource: DataSource = TestableDataSourceCreator.dataSource(config.getConfig("shrine.adapter.query.database"))
    val squerylAdapter: DatabaseAdapter = SquerylDbAdapterSelecter.determineAdapter(config.getString("shrine.shrineDatabaseType"))
    val squerylInitializer: SquerylInitializer = new DataSourceSquerylInitializer(adapterDataSource, squerylAdapter)

    val squerylAdapterTables: AdapterTables = new AdapterTables
    val breakdownTypes = config.getOptionConfigured("breakdownResultOutputTypes",ResultOutputTypes.fromConfig).getOrElse(Set.empty)

    val adapterDao: AdapterDao = new SquerylAdapterDao(squerylInitializer, squerylAdapterTables)(breakdownTypes)

    val adapterQueries: Seq[ShrineQuery] = adapterDao.findQueriesByDomain(domain)

    println(s"Found ${adapterQueries.size} queries for $domain in the adapter's table")

    QepConfigSource.configForBlock(config,getClass.getSimpleName) {

      //filter out any queries that already exist
      val queriesInQep: Set[NetworkQueryId] = QepQueryDb.db.selectAllQepQueries.map(_.networkId).to[Set]

      //turn each ShrineQuery into a QepQuery and store it
      val queriesToInsert: Seq[ShrineQuery] = adapterQueries.filterNot(q => queriesInQep.contains(q.networkId))
      println(s"Will insert ${queriesToInsert.size} rows into the qep's previousQueries table. (${adapterQueries.size - queriesToInsert.size} have matching network ids.)")

      queriesToInsert.map(shrineQueryToQepQuery).foreach(QepQueryDb.db.insertQepQuery)

      //only carry over flags for queries that don't already have flag entries
      val flagsInQep: Set[NetworkQueryId] = QepQueryDb.db.selectMostRecentQepQueryFlagsFor(adapterQueries.map(q => q.networkId).to[Set]).keySet
      val flagsToInsert: Seq[QepQueryFlag] = adapterQueries.filterNot(q => flagsInQep.contains(q.networkId)).flatMap(shrineQueryToQepQueryFlag)
      println(s"Will insert ${flagsToInsert.size} rows into the qep's queryFlags table.")

      //make flags for each ShrineQuery that has them and store that
      flagsToInsert.foreach(QepQueryDb.db.insertQepQueryFlag)
    }

    println("previousQueries transferred from the local adapter.")
  }

  def shrineQueryToQepQuery(shrineQuery: ShrineQuery):QepQuery = {
    val date:Long = toMillis(shrineQuery.dateCreated)
    new QepQuery(
      networkId = shrineQuery.networkId,
      userName = shrineQuery.username,
      userDomain = shrineQuery.domain,
      queryName = shrineQuery.name,
      expression = shrineQuery.queryDefinition.expr.fold("")(expression => expression.toXml.text),
      dateCreated = date,
      deleted = false,
      queryXml = shrineQuery.queryDefinition.toXml.text,
      changeDate = date
    )
  }

  def shrineQueryToQepQueryFlag(shrineQuery: ShrineQuery):Option[QepQueryFlag] = {

    if(shrineQuery.isFlagged){
      Some(new QepQueryFlag(
        networkQueryId = shrineQuery.networkId,
        flagged = shrineQuery.isFlagged,
        flagMessage = shrineQuery.flagMessage.getOrElse(""),
        changeDate = toMillis(shrineQuery.dateCreated)
      ))
    }
    else None
  }
  private def toMillis(xmlGc: XMLGregorianCalendar): Long = xmlGc.toGregorianCalendar.getTimeInMillis
}