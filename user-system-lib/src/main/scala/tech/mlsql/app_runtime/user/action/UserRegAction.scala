package tech.mlsql.app_runtime.user.action

import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.quill_model.{User, UserConstant}
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

    val email = params.get(UserRegAction.Params.EMAIL.name).getOrElse {
      render(400, ActionHelper.msg("Email is required"))
    }

    UserService.findUser(params(UserService.Config.USER_NAME)) match {
      case Some(_) => JSONTool.toJsonStr(List(Map("msg" -> s"User ${params(UserService.Config.USER_NAME)} have been taken.")))
      case None =>
        ctx.run(ctx.query[User].insert(
          _.name -> lift(params(UserService.Config.USER_NAME)),
          _.password -> lift(Md5.md5Hash(params(UserRegAction.Params.PASSWORD.name))),
          _.activated -> lift(UserConstant.IN_ACTIVATED),
          _.createdTime -> lift(System.currentTimeMillis()),
          _.email -> lift(email)
        ))
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
    val EMAIL = Input("email", "")
  }

  def action = "userReg"

  def plugin = PluginItem(UserRegAction.action,
    classOf[UserRegAction].getName, PluginType.action, None)
}
