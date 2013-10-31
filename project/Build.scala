import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "cqrs-es-prototype"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.dreweaster" %% "thespian" % "0.1-SNAPSHOT"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "Drew Easter's Repository" at "http://repo.dreweaster.com/repo/snapshots/"
  )
}
