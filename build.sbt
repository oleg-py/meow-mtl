name := "meow-mtl"
organization := "com.olegpy"
version := "0.1.0"
scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.12", "2.12.4")

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.3",
  "org.typelevel" %% "cats-mtl-core" % "0.2.3",
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
)

scalacOptions ~= (_ filterNot Set(
  "-Xfatal-warnings",
  "-Ywarn-unused:params",
  "-Ywarn-unused:implicits",
))