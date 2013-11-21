organization := "com.mjwall"

name := "accumulo-quickstart"

version := "1.0"

// no scala stuff please

crossPaths := false

autoScalaLibrary := false

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-core" % "1.0.4" intransitive(),
  "org.apache.zookeeper" % "zookeeper" % "3.3.6" intransitive(),
  "org.apache.accumulo" % "accumulo" % "1.5.0" artifacts(Artifact("accumulo", "jar", "tar.gz", "bin")) intransitive()
)

val installPath = taskKey[File]("Directory for install of accumulo and related packages.")

installPath := baseDirectory.value / "install"

val extractDependencies = taskKey[Boolean]("Extract accumulo and related packages into installPath")

def printMethods(o: Object) {
  println("Methods " + o)
  o.getClass.getMethods.map(_.getName).sorted.map(m =>
    println(m)
  )
}


extractDependencies := {
  sbt.IO.createDirectory(installPath.value)
  Build.data((dependencyClasspath in Runtime).value).map ( f =>
    f.getName match {
      case name if name.startsWith("hadoop") => println("Hadoop here: " + f)
      case name if name.startsWith("zookeeper") => println("Zookeeper here: " + f)
      case name if name.startsWith("accumulo") => println("Accumulo here: " + f)
      case name => None //do nothing
    }
  )
  //(update) map {
  //  (updateReport) =>
  //    updateReport.allFiles foreach {
  //      file =>
  //        println(file)
  //    }
 // }
  //(externalDependencyClasspath in Compile).value.map {
  //  cp =>
  //    cp.map {
  //      attributed =>
  //        println(attributed)
  //    }
  //}
  //updateReport.allFiles.map { f =>
  //  println(f)
 // }
  println("----")
  //libraryDependencies.value.map( d =>
  //  if (List("hadoop-core","zookeeper").contains(d.name)) {
  //    println("Unjarring " + d.name)
  //  } else if (d.name == "accumulo") {
  //    println("Artifacts " + d.explicitArtifacts(0))
  //    printMethods(d.explicitArtifacts(0))
  //    println("Untarring " + d.name)
  //  }
  //)
  true
}

val extractOneDependency = taskKey[Boolean]("Extract one dependency")
