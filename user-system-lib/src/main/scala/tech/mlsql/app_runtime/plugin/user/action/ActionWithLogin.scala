package tech.mlsql.app_runtime.plugin.user.action

import tech.mlsql.app_runtime.commons.Input
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.{ActionContext, CustomAction}


abstract class BaseAction extends CustomAction {
  override def run(params: Map[String, String]): String = {
    if (params.contains(ActionRequireLogin.Params.HELP.name)) {
      _help()
    }
    else {
      _run(params)
    }
  }

  def _run(params: Map[String, String]): String

  def _help(): String

  def render(status: Int, content: String): String = {
    val context = ActionContext.context()
    render(context.httpContext.response, status, content)
    ""
  }

  def renderEmpty(): String = {
    render(200, JSONTool.toJsonStr(List(Map())))
    ""
  }
}

abstract class ActionRequireLogin extends BaseAction {
  override def run(params: Map[String, String]): String = {
    if (params.contains(ActionRequireLogin.Params.HELP.name)) {
      _help()
    }
    else {
      val token = params.getOrElse(ActionRequireLogin.Params.ADMIN_TOKEN.name, "")
      if (BasicDBService.adminToken == token) {
        return _run(params)
      }
      val userName = params.getOrElse(UserService.Config.USER_NAME, "")
      val loginToken = params.getOrElse(UserService.Config.LOGIN_TOKEN, "")
      if (UserService.isLogin(userName, loginToken).size > 0) {
        return _run(params)
      }
      render(400, JSONTool.toJsonStr(List(Map("msg" -> "Login or AdminToken is required"))))
    }
  }
}

abstract class ActionRequireResourceAccess extends BaseAction {
  override def run(params: Map[String, String]): String = {
    if (params.contains(ActionRequireLogin.Params.HELP.name)) {
      _help()
    }
    else {
      val canAccess = UserService.checkLoginAndResourceAccess(AddResourceToRoleOrUser.action, params)
      if (!canAccess.access) {
        return render(400, JSONTool.toJsonStr(List(Map("msg" -> canAccess.msg))))
      }
      _run(params)
    }
  }
}

object ActionRequireLogin {

  object Params {
    val HELP = Input("__HELP__", "")
    val ADMIN_TOKEN = Input("admin_token", "")
  }

}

