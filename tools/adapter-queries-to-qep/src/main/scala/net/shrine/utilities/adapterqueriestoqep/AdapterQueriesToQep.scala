package net.shrine.utilities.adapterqueriestoqep

import java.io.File

import com.typesafe.config.ConfigFactory

/**
 * @author dwalend
 * @since 1.21
 */

object AdapterQueriesToQep {
  def main(args: Array[String]): Unit = {
    if(args.length < 2) throw new IllegalArgumentException("Requires at least two arguments, full paths to the adapter-queries-to-qep.conf file and the shrine.conf file.")

    val localConfig = args(0)
    val shrineConfig = args(1)

    val config = ConfigFactory.parseFile(new File(localConfig)).withFallback(ConfigFactory.load(shrineConfig))




  }
}