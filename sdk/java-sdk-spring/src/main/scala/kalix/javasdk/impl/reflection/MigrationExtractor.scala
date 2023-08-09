package kalix.javasdk.impl.reflection

import java.util.Optional

import kalix.javasdk.JacksonMigration
import kalix.javasdk.annotations.Migration

object MigrationExtractor {

  def extractMigration(clazz: Class[_]): Optional[JacksonMigration] = {
    if (clazz.getAnnotation(classOf[Migration]) != null) {
      val migration = clazz
        .getAnnotation(classOf[Migration])
        .value()
        .getConstructor()
        .newInstance()
      Optional.of(migration)
    } else {
      Optional.empty()
    }
  }
}
