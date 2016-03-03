package net.shrine.slick

import java.io.PrintWriter
import java.sql.{DriverManager, Connection}
import java.util.logging.Logger
import javax.naming.InitialContext
import javax.sql.DataSource

import com.typesafe.config.Config

/**
  * @author david 
  * @since 1/26/16
  */
object TestableDataSourceCreator {

  def dataSource(config:Config):DataSource = {

    val dataSourceFrom = config.getString("dataSourceFrom")
    if(dataSourceFrom == "JNDI") {
      val jndiDataSourceName = config.getString("jndiDataSourceName")
      val initialContext:InitialContext = new InitialContext()

      initialContext.lookup(jndiDataSourceName).asInstanceOf[DataSource]

    }
    else if (dataSourceFrom == "testDataSource") {

      val testDataSourceConfig = config.getConfig("testDataSource")
      val driverClassName = testDataSourceConfig.getString("driverClassName")
      val url = testDataSourceConfig.getString("url")

      //Creating an instance of the driver register it. (!) From a previous epoch, but it works.
      Class.forName(driverClassName).newInstance()

      object TestDataSource extends DataSource {
        //todo this is the one used . probably needs to handle passwords
        override def getConnection: Connection = {
          DriverManager.getConnection(url)
        }

        override def getConnection(username: String, password: String): Connection = {
          DriverManager.getConnection(url, username, password)
        }

        //unused methods
        override def unwrap[T](iface: Class[T]): T = ???
        override def isWrapperFor(iface: Class[_]): Boolean = ???
        override def setLogWriter(out: PrintWriter): Unit = ???
        override def getLoginTimeout: Int = ???
        override def setLoginTimeout(seconds: Int): Unit = ???
        override def getParentLogger: Logger = ???
        override def getLogWriter: PrintWriter = ???
      }

      TestDataSource
    }
    else throw new IllegalArgumentException(s"dataSourceFrom config value must be either JNDI or testDataSource, not $dataSourceFrom")
  }

}
