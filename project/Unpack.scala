import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.io.{BufferedInputStream, BufferedOutputStream, File
  ,FileInputStream, InputStream, FileOutputStream, IOException }
import org.apache.commons.io.IOUtils


object Unpack {

  def apply(tarFile: File, dest: File): Unit = {
    dest.mkdir()

    val tarIn = new TarArchiveInputStream(
      new GzipCompressorInputStream(
        new BufferedInputStream(
          new FileInputStream(
            tarFile
          )
        )
      )
    )

    var tarEntry = tarIn.getNextTarEntry()

    while (tarEntry != null) {
      val file = new File(dest, tarEntry.getName())
      if (tarEntry.isDirectory()) {
        file.mkdirs()
      } else {
        file.getParentFile.mkdirs()
        file.createNewFile
        var btoRead = new Array[Byte](1 * 1024)
        var bout = new BufferedOutputStream(new FileOutputStream(file))
        var len = 0
        len = tarIn.read(btoRead)
        while(len != -1) {
          bout.write(btoRead,0,len)
          len = tarIn.read(btoRead)
        }
        bout.close()
        btoRead = null
      }
      tarEntry = tarIn.getNextTarEntry()
    }
    tarIn.close()
  }

}
