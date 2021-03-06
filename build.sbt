
// This file is a result of a partial attempt at switching to sbt from pom.xml (because it supposedly generates docs for scala code).
// Source instrctions: http://www.scala-sbt.org/1.0/docs/Using-Sonatype.html . Not completed.

name := "couchbase-lite-desktop"

scalaVersion := "2.12.3"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "0.9.29"
  ,"ch.qos.logback" % "logback-core" % "0.9.29"
  ,"com.couchbase.lite" % "couchbase-lite-java" % "1.4.1"
  ,"com.couchbase.lite" % "couchbase-lite-java-forestdb" % "1.4.1"
  ,"com.github.sanskrit-coders" % "indic-transliteration_2.12" % "1.8"
  //    ,"com.github.sanskrit-coders" % "sanskrit-lttoolbox" % "0.1"
  ,"com.github.sanskrit-coders" % "db-interface_2.12" % "3.1"
  ,"com.github.sanskrit-coders" % "dict-tools_2.12" % "0.1"
)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

scmInfo := Some(
  ScmInfo(
    url("https://github.com/sanskrit-coders/couchbase-lite-desktop"),
    "scm:git@github.com:sanskrit-coders/couchbase-lite-desktop.git"
  )
)

useGpg := true
publishMavenStyle := true
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
