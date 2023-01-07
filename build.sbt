import xerial.sbt.Sonatype._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

inThisBuild(Seq(
  organization := "com.olegpy",
  scalaVersion := "2.13.3",
  version := "0.5.0",
  crossScalaVersions := Seq("2.12.12", "2.13.3"),
))

lazy val root = project.in(file("."))
  .aggregate(
    core.jvm,
    core.js,
    effects.jvm,
    effects.js,
    monix.jvm,
    monix.js,
  )
  .settings(commonSettings)
  .settings(
    name := "meow-mtl",
    skip in publish := true,
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishTo := None,
  )


lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(name := "meow-mtl-core")
  .settings(commonSettings)
  .settings(libraryDependencies ++= List(
    "com.chuusai" %%% "shapeless" % "2.3.3",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
  ))

lazy val effects = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(name := "meow-mtl-effects")
  .settings(commonSettings)
  .settings(libraryDependencies ++= List(
    "org.typelevel" %%% "cats-effect" % "3.3.0",
    "org.typelevel" %%% "cats-effect-laws" % "3.3.0" % Test,
    "org.typelevel" %%% "cats-effect-testkit" % "3.3.0" % Test
  ))

lazy val monix = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(name := "meow-mtl-monix")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= List(
    "io.monix" %%% "monix-eval" % "3.2.2",
    "org.typelevel" %%% "cats-effect-laws" % "2.2.0" % Test
  ))

def commonSettings = List(

  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("http://github.com/oleg-py/meow-mtl")),

  libraryDependencies ++= Seq(
    "org.typelevel" %%% "cats-mtl"      % "1.0.0",
    "org.typelevel" %%% "cats-laws"     % "2.2.0" % Test,
    "io.monix"      %%% "minitest"      % "2.8.2" % Test,
    "io.monix"      %%% "minitest-laws" % "2.8.2" % Test,
    "org.typelevel" %%% "cats-mtl-laws" % "1.0.0" % Test
  ),

  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),

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
