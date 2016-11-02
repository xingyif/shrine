package net.shrine.slick

import javax.sql.DataSource

/**
  * Created by ty on 7/22/16.
  */
case class CouldNotRunDbIoActionException(dataSource: DataSource, exception: Throwable) extends RuntimeException(exception) {
  override def getMessage:String = s"Could not use the database defined by $dataSource due to ${exception.getLocalizedMessage}"
}
