package tech.mlsql.app_runtime.plugin.user.action

import java.util.UUID

import tech.mlsql.app_runtime.db.action.BasicActionProxy
import tech.mlsql.app_runtime.db.quill_model.DictType
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx._
import tech.mlsql.app_runtime.plugin.user.quill_model.{User, UserSessionDB}
import tech.mlsql.app_runtime.plugin.user.{PluginDB, Session, SystemConfig}
import tech.mlsql.common.utils.Md5
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.{ActionContext, CustomAction}
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}


class UserReg extends CustomAction {

  override def run(params: Map[String, String]): String = {
    if (!UserService.isEnableReg) return JSONTool.toJsonStr(List(Map("msg" -> "User register is not enabled")))

    UserService.findUser(params("name")) match {
      case Some(_) => JSONTool.toJsonStr(List(Map("msg" -> s"User ${params("name")} is exits")))
      case None =>
        ctx.run(ctx.query[User].insert(_.name -> lift(params("name")), _.password -> lift(Md5.md5Hash(params("password")))))
        JSONTool.toJsonStr(List(Map("msg" -> s"User ${params("name")} is registered")))
    }
  }
}

class UserLogin extends CustomAction {

  override def run(params: Map[String, String]): String = {
    val items = (params.get("name"), params.get("password")) match {
      case (Some(name), Some(password)) =>
        UserService.login(name, password) match {
          case Some(_) =>
            val token = UUID.randomUUID().toString
            val session = Session(token, Map())
            UserSessionDB.session.set(name, session)
            List(session)
          case None => List[Session]()
        }
      case (_, _) => List[Session]()
    }
    JSONTool.toJsonStr(items)
  }
}

class IsLogin extends CustomAction {

  override def run(params: Map[String, String]): String = {
    val tokenOpt = params.get("token")
    val passwordOpt = params.get("password")
    val name = params("name")

    def tokenMatch(token: String) = {
      UserSessionDB.session.get(name).headOption match {
        case Some(item) => if (item.token == token) List(item)
        else List()
        case None => List()
      }
    }

    val items = (tokenOpt, passwordOpt) match {
      case (None, Some(_)) =>
        JSONTool.parseJson[List[Session]](new UserLogin().run(params))
      case (Some(token), None) =>
        tokenMatch(token)
      case (Some(token), Some(password)) =>
        tokenMatch(token)
      case (None, None) => List()

    }

    JSONTool.toJsonStr(items)
  }
}

class UserQuery extends CustomAction {
  override def run(params: Map[String, String]): String = {
    val items = params.get("name") match {
      case Some(name) => UserService.findUser(name).headOption match {
        case Some(user) => List(user.copy(password = ""))
        case None => List()
      }
      case None =>
        val context = ActionContext.context()
        val notLogin = JSONTool.parseJson[List[Session]](new IsLogin().run(params)).isEmpty
        if (notLogin) {
          render(context.httpContext.response, 400, "login required")
        }
        ctx.run(ctx.query[User]).toList.map(f => f.name)
    }
    JSONTool.toJsonStr(items)
  }
}

// support redis to cache session

class EnableOrDisableQuery extends CustomAction {
  override def run(params: Map[String, String]): String = {
    val context = ActionContext.context()
    if (!BasicDBService.canAccess(params("admin_token")))
      render(context.httpContext.response, 400, "admin_token is required")

    val value = if (params("enable").toBoolean) SystemConfig.REG_ENABLE.toString
    else SystemConfig.REG_DISABLE.toString
    BasicDBService.addItem(SystemConfig.REG_KEY.toString, value, DictType.SYSTEM_CONFIG)
    JSONTool.toJsonStr(Map("msg" -> "ok"))
  }
}


object UserSystemActionProxy {
  lazy val proxy = new BasicActionProxy(PluginDB.plugin_name)
}

object UserService {

  def isEnableReg = {
    BasicDBService.fetch(SystemConfig.REG_KEY.toString, DictType.SYSTEM_CONFIG).headOption match {
      case Some(item) => item.value == SystemConfig.REG_ENABLE.toString
      case None => false
    }
  }

  def login(name: String, password: String) = {
    ctx.run(users().filter(f => f.name == lift(name) && f.password == lift(Md5.md5Hash(password)))).headOption
  }

  def findUser(name: String) = {
    ctx.run(users().filter(_.name == lift(name))).headOption
  }

  def users() = {
    quote {
      ctx.query[User]
    }
  }

}

object UserQuery {
  def action = "users"

  def plugin = PluginItem(UserQuery.action,
    classOf[UserQuery].getName, PluginType.action, None)
}

object UserReg {
  def action = "userReg"

  def plugin = PluginItem(UserReg.action,
    classOf[UserReg].getName, PluginType.action, None)
}

object EnableOrDisableQuery {
  def action = "controlReg"

  def plugin = PluginItem(EnableOrDisableQuery.action,
    classOf[EnableOrDisableQuery].getName, PluginType.action, None)
}

object UserLogin {
  def action = "userLogin"

  def plugin = PluginItem(UserLogin.action,
    classOf[UserLogin].getName, PluginType.action, None)
}

object IsLogin {
  def action = "isLogin"

  def plugin = PluginItem(IsLogin.action,
    classOf[IsLogin].getName, PluginType.action, None)
}


