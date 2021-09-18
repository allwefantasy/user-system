package tech.mlsql.app_runtime.user.action

import tech.mlsql.serviceframework.platform.form.{FormParams, Input}
import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.quill_model.User
import tech.mlsql.app_runtime.user.Session
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.ActionContext
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}


class UserQuery extends BaseAction {

  override def _run(params: Map[String, String]): String = {
    val items = params.get(UserService.Config.USER_NAME) match {
      case Some(name) => UserService.findUser(name).headOption match {
        case Some(user) => List(user.copy(password = "******"))
        case None => List()
      }
      case None =>
        params.get(UserService.Config.USER_ID) match {
          case Some(id) =>
            ctx.run(ctx.query[User].filter(_.id == lift(id.toInt))).
              map(user => user.copy(password = "******"))
          case None =>
            val context = ActionContext.context()
            val notLogin = JSONTool.parseJson[List[Session]](new IsLoginAction().run(params)).isEmpty
            if (notLogin) {
              render(context.httpContext.response, 400, "login required")
            }
            ctx.run(ctx.query[User]).toList.map(f => f.name)
        }

    }
    JSONTool.toJsonStr(items)
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(UserQuery.Params).toList.reverse)
  }
}


object UserQuery {

  object Params {
    val USER_ID = Input("userId", "")
    val USER_NAME = Input(UserService.Config.USER_NAME, "")
  }

  def action = "users"

  def plugin = PluginItem(UserQuery.action,
    classOf[UserQuery].getName, PluginType.action, None)
}








