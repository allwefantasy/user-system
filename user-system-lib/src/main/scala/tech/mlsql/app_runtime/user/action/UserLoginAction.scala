package tech.mlsql.app_runtime.user.action

import java.util.UUID

import tech.mlsql.app_runtime.user.Session
import tech.mlsql.app_runtime.user.quill_model.UserSessionDB
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.form.{FormParams, Input}
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 19/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class UserLoginAction extends BaseAction with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val msg = "UserName or Password are not matched (Maybe Your account is not activated yet)"
    val items = (params.get(UserLoginAction.Params.USER_NAME.name), params.get(UserLoginAction.Params.PASSWORD.name)) match {
      case (Some(name), Some(password)) =>
        UserService.login(name, password) match {
          case Some(_) =>
            val token = UUID.randomUUID().toString
            val session = Session(token, Map("userName" -> name))
            UserSessionDB.session.set(name, session)
            List(session)
          case None =>
            render(400, ActionHelper.msg(msg))
        }
      case (_, _) => render(400, "UserName or Password is not matched")
    }
    JSONTool.toJsonStr(items)
  }

  override def _help(): String = JSONTool.toJsonStr(
    FormParams.toForm(UserLoginAction.Params).toList.reverse)
}

object UserLoginAction {

  object Params {
    val USER_NAME = Input(UserService.Config.USER_NAME, "")
    val PASSWORD = Input("password", "")
  }

  def action = "userLogin"

  def plugin = PluginItem(UserLoginAction.action,
    classOf[UserLoginAction].getName, PluginType.action, None)
}


class UserLogOutAction extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    UserSessionDB.session.delete(params(UserLogOutAction.Params.USER_NAME.name))
    JSONTool.toJsonStr(Array())
  }

  override def _help(): String = JSONTool.toJsonStr(
    FormParams.toForm(UserLogOutAction.Params).toList.reverse)
}

object UserLogOutAction {

  object Params {
    val USER_NAME = Input(UserService.Config.USER_NAME, "")
  }

  def action = "userLogout"

  def plugin = PluginItem(UserLogOutAction.action,
    classOf[UserLogOutAction].getName, PluginType.action, None)

}
