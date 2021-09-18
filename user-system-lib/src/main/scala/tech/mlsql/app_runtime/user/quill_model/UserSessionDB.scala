package tech.mlsql.app_runtime.user.quill_model

import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.{Session, UserSessionStorage}
import tech.mlsql.common.utils.serder.json.JSONTool


class UserSessionDB extends UserSessionStorage {
  override def set(name: String, session: Session): Unit = {
    val sessionStr = JSONTool.toJsonStr(session)
    ctx.run(userSession(name)).headOption match {
      case Some(_) => ctx.run(userSession(name).update(_.session -> lift(sessionStr), _.createTime -> lift(System.currentTimeMillis())))
      case None => ctx.run(ctx.query[UserSession].insert(
        _.name -> lift(name),
        _.session -> lift(sessionStr),
        _.createTime -> lift(System.currentTimeMillis())
      ))
    }

  }

  override def get(name: String): Option[Session] = {
    val sess = ctx.run(userSession(name)).headOption
    if (sess.isDefined &&
      sess.get.createTime != null &&
      (System.currentTimeMillis() - sess.get.createTime) > 6 * 60 * 60 * 1000) {
      delete(sess.head.name)
      return None
    }
    sess.map(f => JSONTool.parseJson[Session](f.session))
  }


  private def userSession(name: String) = {
    quote {
      ctx.query[UserSession].filter(p => p.name == lift(name))
    }
  }

  override def delete(name: String): Unit = {
    ctx.run(userSession(name)).
      headOption match {
      case Some(sess) =>
        ctx.run(ctx.query[UserSession].filter(_.name == lift(sess.name)).delete)
      case None =>
    }
  }
}

object UserSessionDB {
  val session = new UserSessionDB()
}

