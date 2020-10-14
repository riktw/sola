name := "sola"
version := "1.0"
scalaVersion := "2.11.12"
val spinalVersion = "1.4.0"

libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % spinalVersion,
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % spinalVersion,
  compilerPlugin("com.github.spinalhdl" % "spinalhdl-idsl-plugin_2.11" % spinalVersion)
)

fork := true

lazy val root = Project("root", file("."))
  .dependsOn(spinalSimHelpers)
lazy val spinalSimHelpers = RootProject(uri("https://github.com/riktw/SpinalSimHelpers.git"))
