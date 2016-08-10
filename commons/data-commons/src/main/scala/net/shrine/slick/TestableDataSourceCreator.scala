package net.shrine.slick

import java.io.PrintWriter
import java.sql.{Connection, DriverManager}
import java.util.logging.Logger
import javax.naming.{Context, InitialContext}
import javax.sql.DataSource

import com.typesafe.config.Config
import net.shrine.config.ConfigExtensions

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
      val dataSource = initialContext.lookup(jndiDataSourceName).asInstanceOf[DataSource]
      dataSource
    }
    else if (dataSourceFrom == "testDataSource") {

      val testDataSourceConfig = config.getConfig("testDataSource")
      val driverClassName = testDataSourceConfig.getString("driverClassName")
      val url = testDataSourceConfig.getString("url")

      case class Credentials(username: String,password:String)
      def configToCredentials(config:Config) = new Credentials(config.getString("username"),config.getString("password"))

      val credentials: Option[Credentials] = testDataSourceConfig.getOptionConfigured("credentials",configToCredentials)

      //Creating an instance of the driver register it. (!) From a previous epoch, but it works.
      Class.forName(driverClassName).newInstance()

      object TestDataSource extends DataSource {
        override def getConnection: Connection = {
          credentials.fold(DriverManager.getConnection(url))(credentials =>
            DriverManager.getConnection(url,credentials.username,credentials.password))
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
