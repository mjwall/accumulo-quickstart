import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.compressors.CompressorStreamFactory
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.io.InputStream
import java.io.FileInputStream
import java.io.BufferedInputStream

object Unpack {

  def apply(path: String): Unit = {

    def uncompress(input: BufferedInputStream): InputStream =
      Try(new CompressorStreamFactory().createCompressorInputStream(input)) match {
        case Success(i) => new BufferedInputStream(i)
        case Failure(_) => input
      }

    def extract(input: InputStream): ArchiveInputStream =
      new ArchiveStreamFactory().createArchiveInputStream(input)


    val input = extract(uncompress(new BufferedInputStream(new FileInputStream(path))))
    def stream: Stream[ArchiveEntry] = input.getNextEntry match {
      case null => Stream.empty
      case entry => entry #:: stream
    }

    for(entry <- stream if !entry.isDirectory) {
      println(s"${entry.getName} - ${entry.getSize} bytes")
    }

  }

}
