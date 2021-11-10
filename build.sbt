import Dependencies._

ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "ingest-binary-report-count",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "dev.zio" %% "zio" % "1.0.12",
    libraryDependencies += "dev.zio" %% "zio-streams" % "1.0.12"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
