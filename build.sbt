import scala.collection.mutable.HashMap

organization := "com.mjwall"

name := "accumulo-quickstart"

version := "1.0"

// not building scala stuff

crossPaths := false

autoScalaLibrary := false

libraryDependencies ++= Seq(
  // hadoop and zookeeper dists are not in a maven repo :(
  //"org.apache.hadoop" % "hadoop" % "1.0.4" artifacts(Artifact("hadoop","jar","tar.gz",None, Nil, Some(new URL("http://archive.apache.org/dist/hadoop/core/Hadoop-1.0.4/hadoop-1.0.4.tar.gz")))),
  "org.apache.hadoop" % "hadoop" % "1.2.1" artifacts(Artifact("hadoop","jar","tar.gz",None, Nil, Some(new URL("http://archive.apache.org/dist/hadoop/core/hadoop-1.2.1/hadoop-1.2.1.tar.gz")))),
"org.apache.zookeeper" % "zookeeper" % "3.3.6" artifacts(Artifact("zookeeper", "jar", "tar.gz", None, Nil, Some(new URL("http://archive.apache.org/dist/zookeeper/zookeeper-3.3.6/zookeeper-3.3.6.tar.gz")))) intransitive(), // picked up dependencies somehow
  // accumulo dist is :)
  "org.apache.accumulo" % "accumulo" % "1.5.0" artifacts(Artifact("accumulo", "jar", "tar.gz", "bin")) intransitive()
 )

val installPath = taskKey[File]("Directory for install of accumulo and related packages.")

installPath := baseDirectory.value / "install-home"

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

val ensureJavaHome = taskKey[Boolean]("Check that JAVA_HOME is set")

ensureJavaHome := {
  // todo, better scala check for null.  Not sure this can be
  // since we are running scala
  println(s"JAVA_HOME = ${getJavaHome.value}")
  if (null == getJavaHome.value) {
    false
  } else {
    true
  }
}

val getJavaHome = taskKey[String]("Get the configured JAVA_HOME")

getJavaHome := {
  System.getenv("JAVA_HOME")
}

val extractDependencies = taskKey[Unit]("Extract accumulo and related packages into installPath")

extractDependencies := {
  val dest = installPath.value
  def untar(file: File): String = {
    println(s"Extracting ${file.getName} to ${dest.getName}")
    Unpack.gunzipTar(file, dest)
  }
  if (! ensureJavaHome.value) {
    throw new RuntimeException("JAVA_HOME not set")
  } else if (dest.exists) {
    throw new RuntimeException(s"Install path ${dest} exists, try running removeInstallPath")
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
  }
}

val _getHomePaths = taskKey[HashMap[String,String]]("Returns hashmap of home directories in installPath")

_getHomePaths := {
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
  } else {
    throw new RuntimeException(s"Install path ${rootPath} doesn't exist")
  }
  homes
}

val _getReplaceValues = taskKey[HashMap[String,String]]("Get hashmap of strings to replace")

_getReplaceValues := {
  val replacements = HashMap[String,String]()
  val homePaths = _getHomePaths.value
  val rootPath = installPath.value.getAbsolutePath
  if (homePaths.size > 0) {
    replacements += "REPLACE_JAVA_HOME" -> getJavaHome.value
    replacements += "REPLACE_CLOUD_INSTALL_HOME" -> rootPath
    replacements += "REPLACE_HADOOP_PREFIX" -> homePaths("hadoop")
    replacements += "REPLACE_ZOOKEEPER_HOME" -> homePaths("zookeeper")
    replacements += "REPLACE_ACCUMULO_HOME" -> homePaths("accumulo")
    replacements += "REPLACE_HDFS_PATH" -> s"${rootPath}${java.io.File.separator}hdfs"
    replacements += "REPLACE_MAPRED_PATH" -> s"${rootPath}${java.io.File.separator}mapred"
    replacements += "REPLACE_ZOOKEEPER_DATADIR" -> s"${rootPath}${java.io.File.separator}zk-data"
    replacements += "<value>DEFAULT</value>" -> "<value>QUICKSTARTS_RULE</value>" // instance.secret
  }
  replacements
}

val copyConfigs = taskKey[Unit]("Copies src/main/resources into installPath")

copyConfigs := {
  println("Copying configs mike")
  val rootPath = installPath.value
  if (new File(rootPath, "bin").exists) {
    throw new RuntimeException(s"Looks like copyConfigs has already run, try running removeInstallPath to clean up everything")
  } else {
    val slash = java.io.File.separator
    val appHomes = _getHomePaths.value
    //
    // copy the Accumulo sample configs
    val accumuloHome = appHomes("accumulo")
    val accumuloConf = new File(s"${accumuloHome}${slash}conf")
    val accumuloExampleConf = new File(s"${accumuloConf.getAbsolutePath}${slash}examples${slash}2GB${slash}standalone")
    println(s"Copying example accumulo configs from ${accumuloExampleConf} to ${accumuloConf}")
    sbt.IO.copyDirectory(accumuloExampleConf, accumuloConf, true, true)
    //
    // copy configs that match directories we just unzipped, doing replacement
    val replacements = _getReplaceValues.value
    def overwriteAndReplace(inFile: File, outFile: File) {
        println(s"Copying ${inFile} to ${outFile} and replacing REPLACE_ strings")
        val source = scala.io.Source.fromFile(inFile)
        var inString = source.mkString
        source.close()
        // replace
        replacements.foreach{
          case (key,value) => inString = inString.replaceAll(key, value)
        }
        // write file out
        val newFile = new java.io.FileWriter(outFile, false)
        newFile.write(inString)
        newFile.close
    }
    def copyAppConfigs(appHome: String) {
      val baseDir = new File(appHome).getName
      val templateDir = new File(s"${baseDirectory.value}${slash}src${slash}main${slash}resources${slash}${baseDir}")
      val destDir = new File(s"${appHome}")
      for(subDir <- templateDir.listFiles) {
        if (subDir.isDirectory) {
          for (filename <- subDir.listFiles) {
            // read file, do replacement, write out
            val outFileName = s"${destDir}${slash}${subDir.getName}${slash}${filename.getName}"
            val outFile = new File(outFileName)
            overwriteAndReplace(filename, outFile)
          }
        } else {
          println(s"Unexpected file: ${subDir}")
        }
      }
    }
    copyAppConfigs(appHomes("hadoop"))
    copyAppConfigs(appHomes("zookeeper"))
    copyAppConfigs(appHomes("accumulo"))
    //
    // copy root bin directory
    val binTemplateDir = new File(s"${baseDirectory.value}${slash}src${slash}main${slash}resources${slash}bin")
    val binDestDir = new File(s"${installPath.value}${slash}bin")
    println(s"Copying bin from ${binTemplateDir}  to ${binDestDir}")
    sbt.IO.copyDirectory(binTemplateDir, binDestDir, true, true)
    //
    // replace cloud-env REPLACE strings
    val cloudEnvFile = new File(binDestDir, "cloud-env")
    overwriteAndReplace(cloudEnvFile, cloudEnvFile)
    // replace instance.secret in accumulo-site.xml
    val accumuloSiteFile = new File(s"${accumuloHome}${slash}conf${slash}accumulo-site.xml")
    overwriteAndReplace(accumuloSiteFile, accumuloSiteFile)
    // set all bin directory files executable
    for(binFile <- binDestDir.listFiles) {
      // use a map here
      binFile.setExecutable(true, false)
    }
  }
}

val initAndStart = taskKey[Unit]("format namenode, start hadoop, start zookeeper, init and start accumulo")

initAndStart := {
  Seq(s"${installPath.value}/bin/init-and-start.sh")!
}

addCommandAlias("install", ";extractDependencies;copyConfigs;initAndStart")
