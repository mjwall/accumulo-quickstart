import sbt._
import Keys._

trait JTarUtil {
  def sayHi: Unit
}

class JTarUtilImpl extends JTarUtil {
  def sayHi: Unit = {
    println("Hi from JTarUtil")
  }
}

object tarutil {
  def apply: JTarUtil = new JTarUtilImpl
}

/**
object AccumuloQuickstartBuild extends Build {
  val aqsettings = Defaults.defaultSettings ++ Seq(
    organization := "com.mjwall",
    name         := "accumulo-quickstart",
    version      := "1.0",
    scalaVersion := "2.9.2" // for compiling this file
  )

  //val rootPath = {
  //  baseDirectory.value.getName
  //}
  //val cloudPath = Settings[String]

  val hello = TaskKey[Unit]("hello", "Prints 'Hello World'")

  val helloTask = hello := {
    println("Hello World")
  }

  val unzipArtifacts = TaskKey[Unit]("unzip-artifacts", "Unzips hadoop, zookeper and accumulo")

  val unzipArtifactsTask = unzipArtifacts := {
    val cloudPath = { baseDirectory.value / "cloud" }
    println("basedir " + cloudPath)
    println("basedir2 " + (baseDirectory in run))
    //println("Unzipping artifacts to " + cloudPath)
   // ("mkdir -p " + baseDirectory.value + "/cloud").!
    (dependencyClasspath in Compile) map { (cpEntries) =>
      println(cpEntries)
    }
  }

  lazy val project = Project (
    "project",
    file ("."),
    settings = aqsettings ++ Seq(helloTask, unzipArtifactsTask)
  )
}
**/
