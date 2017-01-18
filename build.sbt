name := "playdb"

description := "A Play Framework DB Module"

organization := "com.github.njlg"

version := "0.1"

homepage := Some(url("https://github.com/njlg/playdb"))

licenses := Seq("Apache License 2.0" -> url("https://opensource.org/licenses/Apache-2.0"))

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

val playVersion = "2.5.10"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % playVersion,
  "com.typesafe.play" %% "play-jdbc" % playVersion
)

publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
publishArtifact in Test := false

lazy val root = (project in file("."))
