package net.shrine.utilities.batchquerier.components

import org.junit.Test
import java.io.File
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date May 29, 2014
 */
final class OutputFileChooserTest extends ShouldMatchersForJUnit {
  @Test
  def testChoose: Unit = {
    import OutputFileChooser.choose

    choose("foo") should equal("foo")
    choose("asjdgf") should equal("asjdgf")

    withFile("foo.csv") {
      choose("foo.csv") should equal("foo.csv.1")
    }

    withFile("foo.csv") {
      withFile("foo.csv.1") {
        choose("foo.csv") should equal("foo.csv.2")
      }
    }

    withFile("foo.csv") {
      withFile("foo.csv.1") {
        withFile("foo.csv.2") {
          withFile("foo.csv.3") {
            choose("foo.csv") should equal("foo.csv.4")
          }
        }
      }
    }
  }
  
  @Test
  def testChooseFile: Unit = {
    import OutputFileChooser.choose

    def file(name: String) = (new File(name)).getCanonicalFile()
    
    choose(file("foo")) should equal(file("foo"))
    choose(file("asjdgf")) should equal(file("asjdgf"))

    withFile("foo.csv") {
      choose(file("foo.csv")) should equal(file("foo.csv.1"))
    }

    withFile("foo.csv") {
      withFile("foo.csv.1") {
        choose(file("foo.csv")) should equal(file("foo.csv.2"))
      }
    }

    withFile("foo.csv") {
      withFile("foo.csv.1") {
        withFile("foo.csv.2") {
          withFile("foo.csv.3") {
            choose(file("foo.csv")) should equal(file("foo.csv.4"))
          }
        }
      }
    }
  }

  private def withFile(name: String)(f: => Any): Unit = {
    val file = new File(name)

    try {
      file.createNewFile()
      file.exists should be(true)
      file.deleteOnExit()

      f
    } finally {
      file.delete()
    }
  }
}