name := "kindle-gen"

organization := "com.gu"

description:= "Converting content to NITF format"

version := "1.0"

scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

// allow scaladoc to process links to classes from libraryDependencies
autoAPIMappings := true
doc / exportJars := true

val log4jVersion = "2.11.0"
val scalaXmlVersion = "1.1.0"

libraryDependencies ++= Seq(
  "com.gu" %% "content-api-client-default" % "12.0",

  "com.typesafe" % "config" % "1.3.3",

  "org.scalaj" %% "scalaj-http" % "2.4.0" /* used to download images */,

  // HTML parsing, cleanup, and conversion to NITF XML
  "org.jsoup" % "jsoup" % "1.11.3",
  "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion,
  "org.scala-lang.modules" %% "scala-parser-combinators" % scalaXmlVersion,

  // logging infrastructure
  "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-jcl" % log4jVersion /* for the Apache HTTP Client in libthrift and aws-java-sdk */,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion /* for the content-api-client and libthrift */,

  // AWS
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.330",

  // test dependencies
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.github.andyglow" %% "scala-xml-diff" % "2.0.3" % "test",
)

dependencyOverrides ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25" /* log4js-slf4j-impl depends on 1.8.0, but that's only compatible with Java 9+ */,
)

//Test / fork := true  // avoid OutOfMemory errors after repeated calls of `sbt test`
Test / logBuffered := false  // enjoy ScalaTest's built-in event buffering algorithm
Test / parallelExecution := false  // avoid exhausting the global execution context in tests opening many connections
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oI")   // show reminder of failed and canceled tests without stack traces

// Guardian's Riff-Raff deployment configuration
enablePlugins(RiffRaffArtifact, JavaAppPackaging)

Universal / topLevelDirectory := None
Universal / packageName := normalizedName.value
riffRaffPackageType := (Universal / dist).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffManifestProjectName := s"Off-platform::${name.value}"
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")
