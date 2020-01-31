package tech.mlsql.app_runtime.plugin.user.quill_model

import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx._
import tech.mlsql.app_runtime.plugin.user.{Session, UserSessionStorage}
import tech.mlsql.common.utils.serder.json.JSONTool


class UserSessionDB extends UserSessionStorage {
  override def set(name: String, session: Session): Unit = {
    val sessionStr = JSONTool.toJsonStr(session)
    ctx.run(userSession(name)).headOption match {
      case Some(_) => ctx.run(userSession(name).update(_.session -> lift(sessionStr)))
      case None => ctx.run(ctx.query[UserSession].insert(
        _.name -> lift(name),
        _.session -> lift(sessionStr)))
    }

  }

  override def get(name: String): Option[Session] = {
    ctx.run(userSession(name)).
      headOption.
      map(f => JSONTool.parseJson[Session](f.session))
  }

  private def userSession(name: String) = {
    quote {
      ctx.query[UserSession].filter(p => p.name == lift(name))
    }
  }
}

object UserSessionDB {
  val session = new UserSessionDB()
}

