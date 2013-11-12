organization := "com.mjwall"

name := "accumulo-quickstart"

version := "1.0"

// no scala stuff please

crossPaths := false

autoScalaLibrary := false

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-core" % "1.0.4" intransitive(),
  "org.apache.zookeeper" % "zookeeper" % "3.3.6" intransitive(),
  "org.apache.accumulo" % "accumulo" % "1.5.0" artifacts(Artifact("accumulo", "tar.gz", "tar.gz", "bin")) intransitive()
)
