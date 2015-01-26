name := "salesforce-dwh"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "xerces" % "xercesImpl" % "2.11.0",
  "org.apache.avro" % "avro" % "1.7.5"
)