import xerial.sbt.Sonatype._


inThisBuild(Seq(
  organization := "com.olegpy",
  scalaVersion := "2.12.6",
  version := "0.2.0",
  crossScalaVersions := Seq("2.11.12", "2.12.6"),
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
    "org.typelevel" %%% "cats-core"   % "1.4.0",
  
  libraryDependencies ++= Seq(
    "com.chuusai"   %%% "shapeless"     % "2.3.3",
    "org.typelevel" %%% "cats-mtl-core" % "0.5.0",
    "org.typelevel" %%% "cats-effect"   % "1.0.0",
    "org.typelevel" %%% "cats-effect-laws" % "1.0.0" % Test,
    "io.monix"      %%% "minitest"      % "2.1.1" % Test,
    "io.monix"      %%% "minitest-laws" % "2.1.1" % Test,
    "org.typelevel" %%% "cats-mtl-laws" % "0.5.0" % Test,
  ),

  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),

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
