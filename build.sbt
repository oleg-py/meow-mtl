name := "meow-mtl"
organization := "com.olegpy"
version := "0.1.0"
scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.3",
  "org.typelevel" %% "cats-mtl-core" % "0.2.3"
)

scalacOptions in Compile -= "-Xfatal-warnings"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")