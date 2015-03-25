name := "wsdl2avro"

organization := "co.datadudes"

version := "0.2-SNAPSHOT"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.10.4", "2.11.4")

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if scala 2.11+ is used, add dependency on scala-xml module
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules"    %% "scala-xml"      % "1.0.2",
        "org.apache.avro"           % "avro"            % "1.7.5",
        "org.specs2"                %% "specs2-junit"   % "2.4.15"    % "test")
    case _ =>
      libraryDependencies.value ++ Seq(
        "org.apache.avro"           % "avro"            % "1.7.5",
        "org.specs2"                %% "specs2-junit"   % "2.4.15"    % "test")
  }
}