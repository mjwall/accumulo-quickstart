import scala.collection.mutable.HashMap

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

val removeInstallPath = taskKey[Unit]("Remove the installPath if it exists")

removeInstallPath := {
  val path = installPath.value
  if (path.exists) {
    println(s"Removing ${path}")
    sbt.IO.delete(path)
  } else {
    println(s"Path ${path} doesn't exist")
  }
}

val checkSSH = taskKey[Boolean]("Check ssh on your box")

checkSSH := {
  SSHWrapper.checkLocalhost
}

val checkJavaHome = taskKey[String]("Check that JAVA_HOME is set")

checkJavaHome := {
  val javaHome = System.getenv("JAVA_HOME")
  // todo, better scala check for null.  Not sure this can be
  // since we are running scala
  if (null == javaHome) {
    throw new RuntimeException("JAVA_HOME is not set")
  }
  javaHome
}

def untar(file: File, dest: File): String = {
  println(s"Extracting ${file.getName} to ${dest.getName}")
  val topDir = Unpack.gunzipTar(file, dest)
  //println(s"Unzipped ${topDir}")
  topDir
}

val extractDependencies = taskKey[HashMap[String, String]]("Extract accumulo and related packages into installPath, returns hash map with home locations")

extractDependencies := {
  val dest = installPath.value
  var hHome = ""
  var zHome = ""
  var aHome = ""
  if (dest.exists) {
    println(s"Install path ${dest} exists, try running removeInstallPath")
    HashMap()
  } else {
    sbt.IO.createDirectory(dest)
    Build.data((dependencyClasspath in Runtime).value).map ( f =>
      f.getName match {
        case name if name.startsWith("hadoop") => hHome = untar(f, dest)
        case name if name.startsWith("zookeeper") => zHome = untar(f, dest)
        case name if name.startsWith("accumulo") => aHome = untar(f, dest)
        case name => None //do nothing
      }
    )
    val homes = HashMap("hadoopHome" -> hHome, "zookeeperHome" -> zHome, "accumuloHome" -> aHome)
    println(s"Homes ${homes}")
    homes
  }
}

val copyConfigs = taskKey[Unit]("Copies src/main/resources into installPath")

copyConfigs := {
  val configDir = new File("src/main/resources")
  println(s"Copying configs from ${configDir} to ${installPath.value}")
  sbt.IO.copyDirectory(configDir, installPath.value, false, true)
  // set bin files to executable
  val binDir = s"${installPath.value}${java.io.File.separator}bin"
  for(binFile <- new File(binDir).listFiles) {
    // use a map here
    binFile.setExecutable(true, false)
  }
}

def replaceStringInFile(filename: String, toReplace: String, replacement: String) = {
  // cd src/main/resources && grep -rl REPLACE .
  // ./accumulo-1.5.0/conf/accumulo-env.sh
  // ./bin/cloud-env
  // ./hadoop-1.0.4/conf/hadoop-env.sh
  // ./hadoop-1.0.4/conf/hdfs-site.xml
  // ./hadoop-1.0.4/conf/mapred-site.xml
  // ./zookeeper-3.3.6/conf/zoo.cfg
  // read file in to string
  //var inString = io.File(filename).slurp
  // replace
  //inString.replaceAll(toReplace, replacement)
  // write file out
  //Path(filename).toFile.writeAll(inString)
}

// check ssh and java

// get tar.gz files

// extract tar.gz files

// copy custom configs

// filter configs replacing home directories

// start hadoop and format

// start zookeeper

// start accumulo

// print message
