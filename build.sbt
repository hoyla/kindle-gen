name := "kindle-gen"

organization := "com.gu"

description:= "Converting content to NITF format"

version := "1.0"

scalaVersion := "2.12.5"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

/* deps for aws lambda */
libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.0"

/*deps for CAPI client*/
libraryDependencies += "com.gu" %% "content-api-client" % "11.51"
/* deps required to use junit test and test watch */
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "junit" % "junit" % "4.12" % "test"

/*deps for simple client*/
//libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.4.3"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.3.0"

/* deps for jsoup and xml (html parsing) */
libraryDependencies += "org.jsoup" % "jsoup" % "1.11.2"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.1.0"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0"

/* deps for external configuration */
libraryDependencies += "com.typesafe" % "config" % "1.3.3"

/* deps for Riff-Raff Guardian deployment tool */
enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

// Copied from https://github.com/guardian/content-api/blob/master/concierge/build.sbt
assemblyMergeStrategy in assembly := {
  case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first
  case "about.html" => MergeStrategy.discard
  case  PathList("models", "intermediate.json") => MergeStrategy.first // Deal with aws sdk containing several time the same file
  case  PathList("models", "model.json") => MergeStrategy.first // Deal with aws sdk containing several time the same file
  // Two shared.thrift files. One from content-atom-models and one from content-entity models
  // both of which are a dependency of content-api-models. We do not need the thrift files so we can exclude these
  // to get around assembly throwing an error.
  case  "shared.thrift" => MergeStrategy.discard
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

/* for auto import in console */
initialCommands in console += """
  import com.gu.kindlegen._;
"""
