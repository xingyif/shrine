package net.shrine.utilities.scanner

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import java.io.File

/**
 * @author clint
 * @date Apr 29, 2013
 */
final class FileNameSourceTest extends ShouldMatchersForJUnit {
  @Test
  def testExists {
    import FileNameSource.exists

    exists("salkfjalgfhalghlsdg.txt") should be(false)

    withFile { file =>
      exists(file.getName) should be(true)
    }
  }

  @Test
  def testNumberOf {
    import FileNameSource.numberOf

    numberOf("foo.csv") should be(None)
    numberOf("foo.1csv") should be(None)
    numberOf("foo1.csv") should be(None)

    numberOf("foo.1.csv") should be(Some(1))
    numberOf("foo.0.csv") should be(Some(0))
    numberOf("foo.99.csv") should be(Some(99))
    numberOf("foo.1000.csv") should be(Some(1000))
  }

  @Test
  def testNumberedFiles {
    import FileNameSource.numberedFiles

    numberedFiles should equal(Nil)

    withFile(file("foo.0.csv")) { f0 =>
      withFile(file("foo.1.csv")) { f1 =>
        withFile(file("foo.99.csv")) { f99 =>
          
          val files = Seq(f0, f1, f99)
          
          numberedFiles.map(_.getCanonicalPath).toSet should equal(files.map(_.getCanonicalPath).toSet)
        }
      }
    }
  }

  @Test
  def testNextOutputFileName {
    import FileNameSource.nextOutputFileName

    nextOutputFileName.startsWith(FileNameSource.base + "-") should be(true)

    def endsWithDotNumberDotCsv(fileName: String) = FileNameSource.endingNumberRegex.pattern.matcher(fileName).matches

    endsWithDotNumberDotCsv(nextOutputFileName) should be(false)

    {
      withFile(file(nextOutputFileName)) { file0 =>
        val nextName = nextOutputFileName
        
        nextName.startsWith(FileNameSource.base + "-") should be(true)
        
        FileNameSource.numberOf(nextName) should be(Some(0))

        withFile(file(nextName)) { file1 =>
          val nextNextName = nextOutputFileName
          
          nextNextName.startsWith(FileNameSource.base + "-") should be(true)
          
          FileNameSource.numberOf(nextNextName) should be(Some(1))
        }
      }
    }
  }

  private def file(name: String) = new File(name)
  
  private def withFile(file: File)(f: File => Any) {
    try {
      file.createNewFile()

      file.exists should be(true)

      f(file)
    } finally {
      file.delete()

      file.exists should be(false)
    }
  }

  def randomString = java.util.UUID.randomUUID.toString
  
  private def withFile(f: File => Any): Unit = withFile(new File(randomString))(f)
}