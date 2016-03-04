package net.shrine.utilities.adapterqueriestoqep

import java.io.File
import javax.sql.DataSource
import javax.xml.datatype.XMLGregorianCalendar

import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.squeryl.SquerylAdapterDao
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

//    println(config)

    val adapterDataSource: DataSource = TestableDataSourceCreator.dataSource(config.getConfig("shrine.adapter.query.database"))
    val squerylAdapter: DatabaseAdapter = SquerylDbAdapterSelecter.determineAdapter(config.getString("shrine.shrineDatabaseType"))
    val squerylInitializer: SquerylInitializer = new DataSourceSquerylInitializer(adapterDataSource, squerylAdapter)

    val squerylAdapterTables: AdapterTables = new AdapterTables
    val breakdownTypes = config.getOptionConfigured("breakdownResultOutputTypes",ResultOutputTypes.fromConfig).getOrElse(Set.empty)

    val adapterDao: AdapterDao = new SquerylAdapterDao(squerylInitializer, squerylAdapterTables)(breakdownTypes)

    val adapterQueries: Seq[ShrineQuery] = adapterDao.findQueriesByDomain(domain)

//    println(s"Found ${adapterQueries.mkString(",\n")}")

    QepConfigSource.configForBlock(config,getClass.getSimpleName) {

      //turn each ShrineQuery into a QepQuery and store it
      //todo filter out any queries that already exist
      adapterQueries.map(shrineQueryToQepQuery).foreach(QepQueryDb.db.insertQepQuery)

      //todo only carry over flags for queries that don't already have flag entries

      //make flags for each ShrineQuery and store that
      adapterQueries.flatMap(shrineQueryToQepQueryFlag).foreach(QepQueryDb.db.insertQepQueryFlag)
    }
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