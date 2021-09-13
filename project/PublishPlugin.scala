import sbt._
import sbt.Keys._
import com.geirsson.CiReleasePlugin
import sbtdynver.DynVerPlugin
import xerial.sbt.Sonatype

/**
 * Default publish settings. Note: disable publishing by default, as Sonatype plugin is automatic. Opt-in to publishing
 * to Sonatype by enabling the PublishSonatype plugin.
 */
object DefaultPublishSettings extends AutoPlugin {

  import DynVerPlugin.autoImport._
  import Sonatype.autoImport._

  override def requires = CiReleasePlugin && SdkVersion
  override def trigger = allRequirements

  override def projectSettings = Seq(
    publish / skip := true,
    publishTo := None,
    dynverSonatypeSnapshots := false,
    scmInfo := (Global / scmInfo).value,
    pomIncludeRepository := (_ => false),
    // Note: need to use the new s01.oss.sonatype.org host
    sonatypeCredentialHost := Sonatype.sonatype01)
}

/**
 * Publish maven artifacts to Sonatype.
 */
object PublishSonatype extends AutoPlugin {

  import DynVerPlugin.autoImport._
  import Sonatype.autoImport._

  override def requires = DefaultPublishSettings

  override def projectSettings = Seq(
    publish / skip := false, // re-enable publishing
    publishTo := sonatypePublishToBundle.value,
    dynverSonatypeSnapshots := true)
}
