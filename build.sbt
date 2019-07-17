import xerial.sbt.Sonatype._


inThisBuild(Seq(
  organization := "com.olegpy",
  scalaVersion := "2.13.0",
  version := "0.3.0-M1",
  crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0"),
))

lazy val root = project.in(file("."))
  .aggregate(meowMtlJVM, meowMtlJS)
  .settings(commonSettings)
  .settings(
    skip in publish := true,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := None,
  )

lazy val meowMtlJS = meowMtl.js
lazy val meowMtlJVM = meowMtl.jvm

lazy val meowMtl = crossProject
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(commonSettings)

def commonSettings = List(
  name := "meow-mtl",

  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("http://github.com/oleg-py/meow-mtl")),

  dependencyOverrides +=
    "org.typelevel" %%% "cats-core"   % "2.0.0-M4",
  
  libraryDependencies ++= Seq(
    "com.chuusai"   %%% "shapeless"     % "2.3.3",
    "org.typelevel" %%% "cats-mtl-core" % "0.6.0",
    "org.typelevel" %%% "cats-effect"   % "2.0.0-M4",
    "org.typelevel" %%% "cats-laws"     % "2.0.0-M4" % Test,
    "org.typelevel" %%% "cats-effect-laws" % "2.0.0-M4" % Test,
    "io.monix"      %%% "minitest"      % "2.5.0" % Test,
    "io.monix"      %%% "minitest-laws" % "2.5.0" % Test,
    "org.typelevel" %%% "cats-mtl-laws" % "0.6.0" % Test,
  ),

  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),

  testFrameworks += new TestFramework("minitest.runner.Framework"),
  scalacOptions --= Seq(
    "-Xfatal-warnings",
    "-Ywarn-unused:params",
    "-Ywarn-unused:implicits",
  ),

  publishTo := sonatypePublishTo.value,
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("oleg-py", "meow-mtl", "oleg.pyzhcov@gmail.com")),
)
