import jassh._
import scala.util.{Try, Success, Failure}

object SSHWrapper {

  // check password less SSH
  def checkLocalhost: Boolean = {
    val user = System.getProperty("user.name")
    Try(SSH.once("localhost", user) {_ execute("""echo `hostname`""")}) match {
      case Success(v) => true
      case Failure(v) => false
    }
  }
}
