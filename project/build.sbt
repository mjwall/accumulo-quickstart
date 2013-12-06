resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-compress" % "1.5",
  "commons-io" % "commons-io" % "2.4",
  "fr.janalyse" %% "janalyse-ssh" % "0.9.10"
)
