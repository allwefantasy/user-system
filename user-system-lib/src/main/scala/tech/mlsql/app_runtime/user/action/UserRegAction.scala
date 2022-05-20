package tech.mlsql.app_runtime.user.action

import net.csdn.ServiceFramwork
import net.csdn.common.settings.Settings
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
    val settings = ServiceFramwork.injector.getInstance(classOf[Settings])
    val activateConfig = settings.getAsBoolean("webplatform.user.activate_by_default", false)
    val activate_by_default = if (activateConfig) {
      UserConstant.ACTIVATED
    } else {
      UserConstant.IN_ACTIVATED
    }

    synchronized {
      UserService.findUser(params(UserService.Config.USER_NAME)) match {
        case Some(_) => JSONTool.toJsonStr(List(Map("msg" -> s"User ${params(UserService.Config.USER_NAME)} have been taken.")))
        case None =>
          ctx.run(ctx.query[User].insert(
            _.name -> lift(params(UserService.Config.USER_NAME)),
            _.password -> lift(Md5.md5Hash(params(UserRegAction.Params.PASSWORD.name))),
            _.activated -> lift(activate_by_default),
            _.createdTime -> lift(System.currentTimeMillis()),
            _.email -> lift(email)
          ))
          val v = if (activateConfig) "" else "Please wait admin to activate it."
          JSONTool.toJsonStr(List(Map("msg" -> s"Your account have been created. ${v}")))
      }
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
