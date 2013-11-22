organization := "com.mjwall"

name := "accumulo-quickstart"

version := "1.0"

// no scala stuff please

crossPaths := false

autoScalaLibrary := false

libraryDependencies ++= Seq(
  // hadoop and zookeeper dists are not in a maven repo :(
  "org.apache.hadoop" % "hadoop" % "1.0.4" artifacts(Artifact("hadoop","jar","tar.gz",None, Nil, Some(new URL("http://archive.apache.org/dist/hadoop/core/hadoop-1.0.4/hadoop-1.0.4.tar.gz")))),
  "org.apache.zookeeper" % "zookeeper" % "3.3.6" artifacts(Artifact("zookeeper", "jar", "tar.gz", None, Nil, Some(new URL("http://archive.apache.org/dist/zookeeper/zookeeper-3.3.6/zookeeper-3.3.6.tar.gz")))) intransitive(), // picked up dependencies somehow
  // accumulo dist is :)
  "org.apache.accumulo" % "accumulo" % "1.5.0" artifacts(Artifact("accumulo", "jar", "tar.gz", "bin")) intransitive(),
  // actually dependencies
  "org.kamranzafar" % "jtar" % "2.2"
)

val installPath = taskKey[File]("Directory for install of accumulo and related packages.")

installPath := baseDirectory.value / "install"

def printMethods(o: Object) {
  println("Methods " + o)
  o.getClass.getMethods.map(_.getName).sorted.map(m =>
    println(m)
  )
}

def untar(file: File, dest: File) {
  println("Untarring " + file + " to " + dest)
}

val extractDependencies = taskKey[Boolean]("Extract accumulo and related packages into installPath")

extractDependencies := {
  sbt.IO.createDirectory(installPath.value)
  Build.data((dependencyClasspath in Runtime).value).map ( f =>
    f.getName match {
      case name if name.startsWith("hadoop") => untar(f, installPath.value)
      case name if name.startsWith("zookeeper") => untar(f, installPath.value)
      case name if name.startsWith("accumulo") => untar(f, installPath.value)
      case name => println("Other: " + f) // None //do nothing
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
