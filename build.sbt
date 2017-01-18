name := "playdb"

description := ""

organization := "com.github.njlg"

version := "0.1"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

val playVersion = "2.5.10"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % playVersion,
  "com.typesafe.play" %% "play-jdbc" % playVersion
)

lazy val root = (project in file("."))
