package net.shrine.status.protocol

import com.typesafe.config.{Config => TsConfig}


import scala.collection.immutable.{Set, Map}

/**
 * This class is for representing a shrine.conf file, note that password data has been
 * redacted.
 *
 * Created by ben on 12/15/15.
 */
//todo SortedMap
case class Config(keyValues:Map[String,String]){

}

object Config {
  def isPassword(key:String):Boolean = {
    key.toLowerCase.contains("password")
  }

  def apply(config:TsConfig):Config = {
    import scala.collection.JavaConverters._
    val entries: Set[(String, String)] = config.entrySet.asScala.to[Set].map(x => (x
      .getKey,x.getValue.render())).filterNot(x => isPassword(x._1))
    val sortedMap: Map[String, String] = entries.toMap
    Config(sortedMap)
  }
}
