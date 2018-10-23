name := "Notechain"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.5"

lazy val root = (project in file("."))

mainClass in Compile := Some("note.NoteChainApp")

libraryDependencies ++= Seq(
  "org.scorexfoundation" %% "scorex-core" % "6a100ea0-SNAPSHOT",
  "org.scorexfoundation" %% "iodb" % "0.3.1"
)
