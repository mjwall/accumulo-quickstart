import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.io.{BufferedInputStream, BufferedOutputStream, File
  ,FileInputStream, InputStream, FileOutputStream, IOException}
import org.apache.commons.io.IOUtils


object Unpack {

  def apply(path: String, dest: File): Unit = {

    def uncompress(input: BufferedInputStream): InputStream =
      Try(new CompressorStreamFactory().createCompressorInputStream(input)) match {
        case Success(i) => new BufferedInputStream(i)
        case Failure(_) => input
      }

    def extract(input: InputStream): ArchiveInputStream =
      new ArchiveStreamFactory().createArchiveInputStream(input)

    def mkdir_p(file: File): Unit = {
      if (!file.exists()) {
        if (file.mkdirs()) {
          println("Directoy created: " + file)
        } else {
         // println("Couldn't make directory: " + file)
        }
      } else {
        //println("Directory exists: " + file)
      }
    }


    val input = extract(uncompress(new BufferedInputStream(new FileInputStream(path))))
    def stream: Stream[ArchiveEntry] = input.getNextEntry match {
      case null => Stream.empty
      case entry => entry #:: stream
    }

    for(entry <- stream) {
      //if (entry.isDirectory) {
      //  println(s"${entry.getName} is directory")
      //} else {
      //  println(s"${entry.getName} - ${entry.getSize} bytes")
     // }
      val file = new File(dest, entry.getName())
      if (entry.isDirectory()) {
        mkdir_p(file)
      } else {
        mkdir_p(file.getParentFile)
        val out = new BufferedOutputStream(new FileOutputStream(file))
        val in = new BufferedInputStream(new FileInputStream(entry))
        try {
          entry match {
            case tar: TarArchiveEntry => {
              println(s"Writing ${tar.getSize} bytes from ${tar.getName}")
              var bitSize = tar.getSize
              var bitses =  stream.read(new byte[bitSize])
              IOUtils.write(bitses, out)
            }
            case _ => { println("Unknown archive entry") }
          }
          out.flush()
        } finally {
          try {
            out.close()
          } catch {
            case e: IOException => {
              println("Error inside tar: " + e.getMessage())
            }
          }
        }
        println("File created: " + file)
      }
    }

  }

}
