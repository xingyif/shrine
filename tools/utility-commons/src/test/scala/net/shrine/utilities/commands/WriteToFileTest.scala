package net.shrine.utilities.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import java.io.File
import scala.io.Source

/**
 * @author clint
 * @date Mar 25, 2013
 */
final class WriteToFileTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {

    val data = "ksaljdkalsjdlkajsdkalsjdlaghdksjdghksfjksldfjlajdsalkdhsalhd\nlksajdkalsjdlasd\n;salkd;saldk"

    withFile { file =>
      val command = WriteTo(file)

      command(data) should equal(())

      withSource(file) { source =>
        val fileContents = source.getLines.reduce(_ + "\n" + _)

        fileContents should equal(data)
      }
    }
  }

  private def withSource(file: File)(f: Source => Any) {
    val source = Source.fromFile(file)

    try {
      f(source)
    } finally {
      source.close()
    }
  }

  private def withFile(f: File => Any) {
    val file = new File("target/foo.tmp")

    try {
      f(file)
    } finally {
      if (file.exists) {
        file.delete()
      }

      file.exists should be(false)
    }
  }
}