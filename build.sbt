organization := "com.mjwall"

name := "accumulo-quickstart"

version := "1.0"

// not building scala stuff

crossPaths := false

autoScalaLibrary := false

libraryDependencies ++= Seq(
  // hadoop and zookeeper dists are not in a maven repo :(
  "org.apache.hadoop" % "hadoop" % "1.0.4" artifacts(Artifact("hadoop","jar","tar.gz",None, Nil, Some(new URL("http://archive.apache.org/dist/hadoop/core/hadoop-1.0.4/hadoop-1.0.4.tar.gz")))),
  "org.apache.zookeeper" % "zookeeper" % "3.3.6" artifacts(Artifact("zookeeper", "jar", "tar.gz", None, Nil, Some(new URL("http://archive.apache.org/dist/zookeeper/zookeeper-3.3.6/zookeeper-3.3.6.tar.gz")))) intransitive(), // picked up dependencies somehow
  // accumulo dist is :)
  "org.apache.accumulo" % "accumulo" % "1.5.0" artifacts(Artifact("accumulo", "jar", "tar.gz", "bin")) intransitive()
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
  //val tu = new JTarUtilImpl
  //tu.untar(file, dest)
  Unpack.apply(file.getAbsolutePath)
}

val extractDependencies = taskKey[Unit]("Extract accumulo and related packages into installPath")

extractDependencies := {
  sbt.IO.createDirectory(installPath.value)
  Build.data((dependencyClasspath in Runtime).value).map ( f =>
    f.getName match {
      case name if name.startsWith("hadoop") => untar(f, installPath.value)
      case name if name.startsWith("zookeeper") => untar(f, installPath.value)
      case name if name.startsWith("accumulo") => untar(f, installPath.value)
      case name => None //do nothing
    }
  )
}
