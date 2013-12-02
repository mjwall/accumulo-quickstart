import sbt._
import Keys._

import java.util.zip.GZIPInputStream
import java.io.{BufferedInputStream, BufferedOutputStream, File
  ,FileInputStream, FileOutputStream, IOException}
import org.kamranzafar.jtar.{TarEntry, TarInputStream}

trait JTarUtil {
  def untar(src: File, dest: File): Unit
}

class JTarUtilImpl extends JTarUtil {

  def untar(src: File, dest: File): Unit = {
    println("Untarring " + src + " to " + dest)

    val BUFFER_SIZE = 8 * 1024
    try {
      val tarArchiveInputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(src), BUFFER_SIZE))
      var entry: TarEntry = null

      while ({entry = tarArchiveInputStream.getNextEntry; entry != null}) {
        val file = new File(dest, entry.getName())
        if (entry.isDirectory()) {
          if (!file.exists()) {
            if (file.mkdirs()) {
              println("Directoy created: " + file)
            } else {
              println("Couldn't make directory: " + file)
            }
          } else {
            println("Directory exists: " + file)
          }
        } else {
          val out = new BufferedOutputStream(new FileOutputStream(file))
          try {
            FileUtils.copyFile(in, out);
            out.flush()
          } finally {
            try {
              out.close()
            } catch (IOException e) {
              println("Error inside tar: " + e.getMessage())
            }
          }
          println("File created: " + file)
        }
      }
    } catch (IOException e) {
      println("Error " + e.getMessage())
    } finally {
      if (tarArchiveInputStream != null) {
        try {
          tarArchiveInputStream.close()
        } catch (IOException e) {
          println("Error closing " + e.getMessage())
        }
      } else if (fileInputStream != null) {
        try {
          fileInputStream.close()
        } catch (IOException e) {
          println("Error closing stream2 " + e.getMessage())
        }
      }
    }
  }


}

//object tarutil {
//  def apply: JTarUtil = new JTarUtilImpl
//}
