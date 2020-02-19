package tech.mlsql.app_runtime.plugin.user.action

import java.util.UUID

import tech.mlsql.app_runtime.commons.{FormParams, Input}
import tech.mlsql.app_runtime.plugin.user.Session
import tech.mlsql.app_runtime.plugin.user.quill_model.UserSessionDB
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 19/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class UserLoginAction extends BaseAction {
  override def _run(params: Map[String, String]): String = {
    val items = (params.get(UserLoginAction.Params.USER_NAME.name), params.get(UserLoginAction.Params.PASSWORD.name)) match {
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
