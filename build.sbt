name := "kindle-gen"

organization := "com.gu"

description:= "Converting content to NITF format"

version := "1.0"

scalaVersion := "2.12.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

/* deps for aws lambda */
libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.1.0"

/*deps for CAPI client*/
libraryDependencies += "com.gu" %% "content-api-client" % "11.29"
/* deps required to use junit test and test watch */
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "junit" % "junit" % "4.10" % "test"
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

/*deps for simple client*/
//libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.4.3"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.3.0"

/* deps for jsoup and xml (html parsing) */
libraryDependencies += "org.jsoup" % "jsoup" % "1.7.2"
libraryDependencies += "org.scala-lang" % "scala-xml" % "2.11.0-M4"

/* deps for Riff-Raff Guardian deployment tool */
enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

/* for auto import in console */
initialCommands in console := "import com.gu.kindlegen._"