name := "wsdl2avro"

organization := "co.datadudes"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.10.4", "2.11.4")

publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := <url>https://github.com/datadudes/wsdl2avro</url>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>http://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:datadudes/wsdl2avro.git</url>
    <connection>scm:git:git@github.com:datadudes/wsdl2avro.git</connection>
  </scm>
  <developers>
    <developer>
      <id>DandyDev</id>
      <name>Daan Debie</name>
      <url>http://dandydev.net</url>
    </developer>
    <developer>
      <id>mkrcah</id>
      <name>Marcel Krcah</name>
      <url>http://marcelkrcah.net</url>
    </developer>
  </developers>

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

releaseSettings