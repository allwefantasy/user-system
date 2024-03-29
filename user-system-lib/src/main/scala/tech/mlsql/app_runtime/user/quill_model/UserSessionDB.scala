package tech.mlsql.app_runtime.user.quill_model

import net.csdn.ServiceFramwork
import net.csdn.common.settings.Settings
import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.{Session, UserSessionStorage}
import tech.mlsql.common.utils.distribute.socket.server.JavaUtils
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

  def sessionTimeout: Long = {
    val _settings = ServiceFramwork.injector.getInstance(classOf[Settings])
    // 8d 8h 8m
    val t = _settings.get("session.timeout", "8h")
    JavaUtils.timeStringAsSec(t)
  }

  override def get(name: String): Option[Session] = {

    val sess = ctx.run(userSession(name)).headOption
    if (sess.isDefined &&
      (System.currentTimeMillis() - sess.get.createTime) > sessionTimeout * 1000) {
      delete(sess.head.name)
      return None
    }
    //todo: here we can optimize do not update the session every access
    ctx.run(ctx.query[UserSession].filter(_.name == lift(name)).update(_.createTime -> lift(System.currentTimeMillis())))
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

