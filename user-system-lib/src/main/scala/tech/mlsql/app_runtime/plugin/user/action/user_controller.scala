package tech.mlsql.app_runtime.plugin.user.action

import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx._
import tech.mlsql.app_runtime.plugin.user.quill_model.{User, UserSessionDB}
import tech.mlsql.app_runtime.plugin.user.{Session, SystemConfig}
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.{ActionContext, CustomAction}
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}


class UserQuery extends CustomAction {
  override def run(params: Map[String, String]): String = {
    val items = params.get(UserService.Config.USER_NAME) match {
      case Some(name) => UserService.findUser(name).headOption match {
        case Some(user) => List(user.copy(password = ""))
        case None => List()
      }
      case None =>
        val context = ActionContext.context()
        val notLogin = JSONTool.parseJson[List[Session]](new IsLoginAction().run(params)).isEmpty
        if (notLogin) {
          render(context.httpContext.response, 400, "login required")
        }
        ctx.run(ctx.query[User]).toList.map(f => f.name)
    }
    JSONTool.toJsonStr(items)
  }
}



object UserQuery {
  def action = "users"

  def plugin = PluginItem(UserQuery.action,
    classOf[UserQuery].getName, PluginType.action, None)
}








