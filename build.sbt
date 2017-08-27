name := "kindle-gen"

organization := "com.gu"

description:= "Converting content to NITF format"

version := "1.0"

scalaVersion := "2.11.11"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

/* deps for aws lambda */
libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.1.0"

/*deps for CAPI client*/
libraryDependencies += "com.gu" %% "content-api-client" % "11.22"


/* deps required to use junit test and test watch */
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "junit" % "junit" % "4.10" % "test"
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

/* wrapper around jodatime */
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"

/* deps for Riff-Raff Guardian deployment tool */
enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

/* for auto import in console */
initialCommands in console := "import com.gu.kindlegen._"