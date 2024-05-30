import sbt._
import sbt.Keys._
import com.geirsson.CiReleasePlugin
import com.jsuereth.sbtpgp.PgpKeys.publishSigned
import com.jsuereth.sbtpgp.PgpKeys.publishSignedConfiguration
import sbtdynver.DynVerPlugin
import sbtdynver.DynVerPlugin.autoImport.dynverSonatypeSnapshots
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport.sonatypeProfileName

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Default publish settings. Note: disable publishing by default, as Sonatype plugin is automatic. Opt-in to publishing
 * to Sonatype by enabling the Publish plugin.
 */
object DefaultPublishSettings extends AutoPlugin {

  override def requires = SdkVersion
  override def trigger = allRequirements

  override def projectSettings = Seq(publish / skip := true, publishTo := None, pomIncludeRepository := (_ => false))
}

/**
 * Publish maven artifacts to the Cloudsmith repository.
 */
object Publish extends AutoPlugin {
  override def requires = plugins.JvmPlugin && CommonSettings && DefaultPublishSettings
  override def trigger = AllRequirements

  private lazy val beforePublishTask = taskKey[Unit]("setup before publish")
  private lazy val beforePublishDone = new AtomicBoolean(false)

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      publish / skip := false, // re-enable publishing
      dynverSonatypeSnapshots := false, // don't append -SNAPSHOT
      sonatypeProfileName := "com.typesafe",
      beforePublishTask := beforePublish(isSnapshot.value),
      publishSigned := publishSigned.dependsOn(beforePublishTask).value,
      publishTo :=
        (if (isSnapshot.value)
           Some("Cloudsmith API".at("https://maven.cloudsmith.io/lightbend/akka-snapshots/"))
         else
           Some("Cloudsmith API".at("https://maven.cloudsmith.io/lightbend/akka/"))),
      credentials ++= cloudsmithCredentials(validate = false))

  private def beforePublish(snapshot: Boolean): Unit = {
    if (beforePublishDone.compareAndSet(false, true)) {
      CiReleasePlugin.setupGpg()
      if (!snapshot)
        cloudsmithCredentials(validate = true)
    }
  }

  private def cloudsmithCredentials(validate: Boolean): Seq[Credentials] = {
    (sys.env.get("PUBLISH_USER"), sys.env.get("PUBLISH_PASSWORD")) match {
      case (Some(user), Some(password)) =>
        Seq(Credentials("Cloudsmith API", "maven.cloudsmith.io", user, password))
      case _ =>
        if (validate)
          throw new Exception("Publishing credentials expected in `PUBLISH_USER` and `PUBLISH_PASSWORD`.")
        else
          Nil
    }
  }
}
