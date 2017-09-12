package net.shrine.messagequeueservice

import net.shrine.spray.DefaultJsonSupport
import org.json4s.{DefaultFormats, Formats}

import scala.util.Try

/**
  * Created by yifan on 9/8/17.
  */

trait Message extends DefaultJsonSupport {
  override implicit def json4sFormats: Formats = DefaultFormats
  def complete(): Try[Unit]
  def getContents: String
}

object Message {
  val contentsKey = "contents"
}