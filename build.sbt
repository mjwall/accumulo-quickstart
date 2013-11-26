//import java.io.BufferedInputStream
//import java.io.BufferedOutputStream
//import java.io.File
//import java.io.FileInputStream
//import java.io.FileOutputStream
//import java.io.IOException
//import java.util.zip.GZIPInputStream

//import org.xeustechnologies.jtar.TarEntry
//import org.xeustechnologies.jtar.TarInputStream

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
  // assumes gzipped
  println("Untarring " + file + " to " + dest)
  // boolean result = false
  // FileInputStream fileInputStream = null
  // TarInputStream tarArchiveInputStream = null
  // //String LOG_TAG = JTarUtils.class.getSimpleName();
  // int BUFFER_SIZE = 8 * 1024
  // try {
  //   fileInputStream = new FileInputStream(file);
  //   //tarArchiveInputStream = (isGZipped) ?
  //   //new TarInputStream(new GZIPInputStream(fileInputStream, BUFFER_SIZE)) :
  //   //new TarInputStream(new BufferedInputStream(fileInputStream, BUFFER_SIZE));
  //   //result = untar(tarArchiveInputStream, outputDir);
  //   tarArchiveInputStream = new TarInputStream(new GZIPInputStream(fileInputStream, BUFFER_SIZE))
  //   try {
  //     TarEntry entry;
  //     while ((entry = tarArchiveInputStream.getNextEntry()) != null) {
  //       final File file = new File(outputDir, entry.getName());
  //       if (entry.isDirectory()) {
  //         if (!file.exists()) {
  //           if (file.mkdirs()) {
  //             //Log.d(LOG_TAG, "%s directory created", file);
  //           } else {
  //             //Log.w(LOG_TAG, "%s failure to create directory.", file);
  //             return false;
  //           }
  //         } else {
  //           //Log.w(LOG_TAG, "%s directory is already created", file);
  //         }
  //       } else {
  //         BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
  //         try {
  //           FileUtils.copyFile(tarArchiveInputStream, out);
  //           out.flush();
  //         } finally {
  //           try {
  //             out.close();
  //           } catch (IOException e) {
  //             //Log.e(LOG_TAG, e);
  //           }
  //         }
  //         //Log.d(LOG_TAG, "%s file created", file);
  //       }
  //     }
  //     result = true;
  //   } catch (IOException e) {
  //     //Log.e(LOG_TAG, e);
  //   }
  // } catch (IOException e) {
  //   //Log.e(LOG_TAG, e);
  // } finally {
  //   if (tarArchiveInputStream != null) {
  //     try {
  //       tarArchiveInputStream.close();
  //     } catch (IOException e) {
  //       //Log.e(LOG_TAG, e);
  //     }
  //   } else if (fileInputStream != null) {
  //     try {
  //       fileInputStream.close();
  //     } catch (IOException e) {
  //       //Log.e(LOG_TAG, e);
  //     }
  //   }
  // }
  // return result;
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

val extractOneDependency = taskKey[Boolean]("Extract one dependency")
