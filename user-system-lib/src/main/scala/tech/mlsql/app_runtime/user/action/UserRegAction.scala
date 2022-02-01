package tech.mlsql.app_runtime.user.action

import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.quill_model.User
import tech.mlsql.common.utils.Md5
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.form.{FormParams, Input}
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 19/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class UserRegAction extends BaseAction with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    if (!UserService.isEnableReg) return JSONTool.toJsonStr(List(Map("msg" -> "User register is not enabled")))

    UserService.findUser(params(UserService.Config.USER_NAME)) match {
      case Some(_) => JSONTool.toJsonStr(List(Map("msg" -> s"User ${params(UserService.Config.USER_NAME)} is exits")))
      case None =>
        ctx.run(ctx.query[User].insert(_.name -> lift(params(UserService.Config.USER_NAME)), _.password -> lift(Md5.md5Hash(params(UserRegAction.Params.PASSWORD.name)))))
        JSONTool.toJsonStr(List(Map("msg" -> s"User ${params(UserService.Config.USER_NAME)} is registered")))
    }
  }

  override def _help(): String = JSONTool.toJsonStr(
    FormParams.toForm(UserRegAction.Params).toList.reverse)
}

object UserRegAction {

  object Params {
    val USER_NAME = Input(UserService.Config.USER_NAME, "")
    val PASSWORD = Input("password", "")
  }

  def action = "userReg"

  def plugin = PluginItem(UserRegAction.action,
    classOf[UserRegAction].getName, PluginType.action, None)
}
