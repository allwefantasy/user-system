package tech.mlsql.app_runtime.plugin.user.action

import tech.mlsql.app_runtime.db.quill_model.DictType
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx._
import tech.mlsql.app_runtime.plugin.user.SystemConfig
import tech.mlsql.app_runtime.plugin.user.quill_model.User
import tech.mlsql.common.utils.Md5
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.CustomAction


class UserReg extends CustomAction {

  override def run(params: Map[String, String]): String = {
    if (!UserService.isEnableReg) JSONTool.toJsonStr(List(Map("msg" -> "User register is not enabled")))

    UserService.findUser(params("name")) match {
      case Some(_) => JSONTool.toJsonStr(List(Map("msg" -> s"User ${params("name")} is exits")))
      case None =>
        ctx.run(ctx.query[User].insert(_.name -> lift(params("name")), _.password -> lift(Md5.md5Hash(params("password")))))
        JSONTool.toJsonStr(List(Map("msg" -> s"User ${params("name")} is registered")))
    }
  }
}

class UserQuery extends CustomAction {
  override def run(params: Map[String, String]): String = {
    val names = ctx.run(ctx.query[User]).toList.map(f => f.name)
    JSONTool.toJsonStr(names)
  }
}

class EnableOrDisableQuery extends CustomAction {
  override def run(params: Map[String, String]): String = {
    val value = if (params("enable").toBoolean) SystemConfig.REG_ENABLE.toString
    else SystemConfig.REG_DISABLE.toString
    BasicDBService.addItem(SystemConfig.REG_KEY.toString, value, DictType.SYSTEM_CONFIG)
    JSONTool.toJsonStr(Map("msg" -> "ok"))
  }
}


object UserService {


  def isEnableReg = {
    BasicDBService.fetch(SystemConfig.REG_KEY.toString, DictType.SYSTEM_CONFIG).headOption match {
      case Some(item) => item.value == SystemConfig.REG_ENABLE.toString
      case None => false
    }
  }

  def findUser(name: String) = {
    ctx.run(ctx.query[User].filter(_.name == lift(name))).headOption
  }

}
