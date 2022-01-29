package tech.mlsql.app_runtime.user.action

import tech.mlsql.app_runtime.user.Session
import tech.mlsql.app_runtime.user.quill_model.UserSessionDB
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.form.{FormParams, Input}
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 19/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class IsLoginAction extends BaseAction with ActionInfo{
  override def _run(params: Map[String, String]): String = {
    val tokenOpt = params.get(UserService.Config.LOGIN_TOKEN)
    val passwordOpt = params.get(IsLoginAction.Params.PASSWORD.name)
    val name = params(IsLoginAction.Params.USER_NAME.name)

    def tokenMatch(token: String) = {
      UserSessionDB.session.get(name).headOption match {
        case Some(item) =>
          if (item.token == token) List(item)
          else List()
        case None => List()
      }
    }

    val items = (tokenOpt, passwordOpt) match {
      case (None, Some(_)) =>
        JSONTool.parseJson[List[Session]](new UserLoginAction().run(params))
      case (Some(token), None) =>
        tokenMatch(token)
      case (Some(token), Some(password)) =>
        tokenMatch(token)
      case (None, None) => List()

    }

    JSONTool.toJsonStr(items)
  }

  override def _help(): String = {
    JSONTool.toJsonStr(
      FormParams.toForm(IsLoginAction.Params).toList.reverse)
  }
}

object IsLoginAction {

  object Params {
    val USER_NAME = Input(UserService.Config.USER_NAME, "")
    val PASSWORD = Input("password", "")
  }

  def action = "isLogin"

  def plugin = PluginItem(IsLoginAction.action,
    classOf[IsLoginAction].getName, PluginType.action, None)
}
