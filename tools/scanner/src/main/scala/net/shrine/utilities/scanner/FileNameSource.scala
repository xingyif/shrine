package net.shrine.utilities.scanner

import java.text.SimpleDateFormat
import java.io.File
import scala.util.matching.Regex
import java.io.FilenameFilter
import scala.util.Try

/**
 * @author clint
 * @since Apr 29, 2013
 */
object FileNameSource {
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  private[scanner] val endingNumberRegex = """.*\.(\d+)\.csv$""".r

  private[scanner] val base = "scanner-output"
  
  def nextOutputFileName: String = {
    val dateString = dateFormat.format(new java.util.Date)

    val baseName = s"${ base }-$dateString"

    val baseNameWithExtension = baseName + ".csv"

    if (exists(baseNameWithExtension)) {

      val fileNumber = numberedFiles match {
        case Nil => 0
        case matches => {
          val highestNumberedFile = matches.sorted.reverse.head

          numberOf(highestNumberedFile.getName).map(_ + 1).getOrElse(0)
        }
      }

      s"$baseName.$fileNumber.csv"

    } else { baseNameWithExtension }
  }

  private[scanner] def exists(fileName: String) = (new File(fileName)).exists
  
  private[scanner] def numberOf(fileName: String): Option[Int] = fileName match {
    case endingNumberRegex(i) => Try(i.toInt).toOption
    case _ => None
  }

  private[scanner] def numberedFiles: Seq[File] = {
    val cwd = new File(".")

    val fileNameFilter = new FilenameFilter {
      override def accept(enclosingDir: File, fileName: String): Boolean = {
        endingNumberRegex.pattern.matcher(fileName).matches
      }
    }

    cwd.listFiles(fileNameFilter)
  }
}