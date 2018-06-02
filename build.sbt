inThisBuild(Seq(
  organization := "com.olegpy",
  scalaVersion := "2.12.4",
  version := "0.1.0-SNAPSHOT",
  crossScalaVersions := Seq("2.11.12", "2.12.4"),
))

lazy val root = project.in(file("."))
  .aggregate(meowMtlJVM, meowMtlJS)
  .dependsOn(meowMtlJVM) // TODO: Hack to make intellij work
  .settings(
    publish := {},
    publishLocal := {},
  )

lazy val meowMtlJS = meowMtl.js
lazy val meowMtlJVM = meowMtl.jvm

lazy val meowMtl = crossProject
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(
    name := "meow-mtl",

    libraryDependencies ++= Seq(
      "com.chuusai"   %%% "shapeless"     % "2.3.3",
      "org.typelevel" %%% "cats-mtl-core" % "0.2.3",
      "org.typelevel" %%% "cats-effect"   % "1.0.0-RC2-d7181dc",
      "io.monix"      %%% "minitest"      % "2.1.1" % Test,
    ),

    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),

    testFrameworks += new TestFramework("minitest.runner.Framework"),

    scalacOptions --= Seq(
      "-Xfatal-warnings",
      "-Ywarn-unused:params",
      "-Ywarn-unused:implicits",
    ),
  )