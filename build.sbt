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

installPath := baseDirectory.value / "cloud-install"

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

val checkJavaHome = taskKey[Boolean]("Check that JAVA_HOME is set")

checkJavaHome := {
  // todo, better scala check for null.  Not sure this can be
  // since we are running scala
  if (null == getJavaHome) {
    false
  }
  true
}

val getJavaHome = taskKey[String]("Get the configured JAVA_HOME")

getJavaHome := {
  System.getenv("JAVA_HOME")
}

val extractDependencies = taskKey[Boolean]("Extract accumulo and related packages into installPath")

extractDependencies := {
  val dest = installPath.value
  def untar(file: File): String = {
    println(s"Extracting ${file.getName} to ${dest.getName}")
    Unpack.gunzipTar(file, dest)
  }
  if (dest.exists) {
    println(s"Install path ${dest} exists, try running removeInstallPath")
    false
  } else {
    sbt.IO.createDirectory(dest)
    Build.data((dependencyClasspath in Runtime).value).map ( f =>
      f.getName match {
        case name if name.startsWith("hadoop") => untar(f)
        case name if name.startsWith("zookeeper") => untar(f)
        case name if name.startsWith("accumulo") => untar(f)
        case name => None //do nothing
      }
    )
    true
  }
}

val copyConfigs = taskKey[Boolean]("Copies src/main/resources into installPath")

copyConfigs := {
  if (new File(installPath.value, "bin").exists) {
    println(s"Looks like copyConfigs has already run, try running removeInstallPath to clean up everything")
    false
  } else {
    val configDir = new File("src/main/resources")
    println(s"Copying configs from ${configDir} to ${installPath.value}")
    sbt.IO.copyDirectory(configDir, installPath.value, true, true)
    // set bin files to executable
    val binDir = s"${installPath.value}${java.io.File.separator}bin"
    for(binFile <- new File(binDir).listFiles) {
      // use a map here
      binFile.setExecutable(true, false)
    }
    true
  }
}

val getHomePaths = taskKey[HashMap[String,String]]("Returns hashmap of home directories in installPath")

getHomePaths := {
  val rootPath = installPath.value
  var homes = HashMap[String, String]()
  homes += "quickstart" -> rootPath.getAbsolutePath
  if (rootPath.exists) {
    for(dir <- installPath.value.listFiles) {
      if (dir.isDirectory) {
        dir.getName match {
          case name if name.startsWith("hadoop") => homes += "hadoop" -> dir.getAbsolutePath
          case name if name.startsWith("zookeeper") => homes += "zookeeper" -> dir.getAbsolutePath
          case name if name.startsWith("accumulo") => homes += "accumulo" -> dir.getAbsolutePath
          case name => None //do nothing
        }
      }
    }
  }
  homes
}

val replacePathsInConfigs = taskKey[Boolean]("Replaces Paths in config files")

replacePathsInConfigs := {
  // TODO: think about 2 config directory, one that needs replacement and one that doesn't
  // cd src/main/resources && grep -rl REPLACE .
  // ./accumulo-1.5.0/conf/accumulo-env.sh
  // ./bin/cloud-env
  // ./hadoop-1.0.4/conf/hadoop-env.sh
  // ./hadoop-1.0.4/conf/hdfs-site.xml
  // ./hadoop-1.0.4/conf/mapred-site.xml
  // ./zookeeper-3.3.6/conf/zoo.cfg
  val rootPath = installPath.value
  val files = List(
    new File(rootPath, "bin/cloud-env"),
    new File(rootPath, "hadoop-1.0.4/conf/hadoop-env.sh"),
    new File(rootPath, "hadoop-1.0.4/conf/hdfs-site.xml"),
    new File(rootPath, "hadoop-1.0.4/conf/mapred-site.xml"),
    new File(rootPath, "zookeeper-3.3.6/conf/zoo.cfg"),
    new File(rootPath, "accumulo-1.5.0/conf/accumulo-env.sh")
  )
  val replacements = getReplaceValues.value
  for(f <- files) {
    val filename = f.getAbsolutePath
    println(s"replacing in ${f}")
    // read file in to string
    val source = scala.io.Source.fromFile(filename)
    var inString = source.mkString
    source.close()
    // replace
    replacements.foreach{
      case (key,value) => inString = inString.replaceAll(key, value)
    }
    // write file out
    f.delete
    val newFile = new java.io.FileWriter(filename, false)
    newFile.write(inString)
    newFile.close
  }
  true
}

val getReplaceValues = taskKey[HashMap[String,String]]("Get hashmap of strings to replace")

getReplaceValues := {
  val replacements = HashMap[String,String]()
  val homePath = getHomePaths.value
  val rootPath = installPath.value.getAbsolutePath
  replacements += "REPLACE_JAVA_HOME" -> getJavaHome.value
  replacements += "REPLACE_CLOUD_INSTALL_HOME" -> rootPath
  replacements += "REPLACE_HADOOP_PREFIX" -> homePath("hadoop")
  replacements += "REPLACE_ZOOKEEPER_HOME" -> homePath("zookeeper")
  replacements += "REPLACE_ACCUMULO_HOME" -> homePath("accumulo")
  replacements += "REPLACE_HDFS_PATH" -> s"${rootPath}${java.io.File.separator}hdfs"
  replacements += "REPLACE_MAPRED_PATH" -> s"${rootPath}${java.io.File.separator}mapred"
  replacements += "REPLACE_ZOOKEEPER_DATADIR" -> s"${rootPath}${java.io.File.separator}zk-data"
  replacements
}

val initAndStart = taskKey[Unit]("format namenode, start hadoop, start zookeeper, init and start accumulo")

initAndStart := {
  Seq(s"${installPath.value}/bin/init-and-start.sh")!!
}

addCommandAlias("install", ";extractDependencies;copyConfigs;replacePathsInConfigs;initAndStart")
//addCommandAlias("install", ";extractDependencies;copyConfigs;replacePathsInConfigs")

// check ssh and java

// get tar.gz files

// extract tar.gz files

// copy custom configs

// filter configs replacing home directories

// start hadoop and format

// start zookeeper

// start accumulo

// print message
