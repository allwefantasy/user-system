package tech.mlsql.app_runtime.plugin.user

/**
 * 30/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
trait UserSessionStorage {
  def set(name: String, session: Session): Unit

  def get(name: String): Option[Session]
}

case class Session(token: String, params: Map[String, String])


