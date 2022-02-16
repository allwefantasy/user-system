package tech.mlsql.app_runtime.user.action

import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.Session
import tech.mlsql.app_runtime.user.quill_model.{UserLoginTracker, UserSessionDB}
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.form.{FormParams, Input}
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

import java.util.UUID

/**
 * 19/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class UserLoginAction extends BaseAction with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val msg = "UserName or Password are not matched (Maybe Your account is not activated yet)"
    val items = (params.get(UserLoginAction.Params.USER_NAME.name), params.get(UserLoginAction.Params.PASSWORD.name)) match {
      case (Some(name), Some(password)) =>
        val FIVE_M = 1000 * 60 * 5
        val HALF_HOUR = 1000 * 60 * 30
        val userName = params(UserLoginAction.Params.USER_NAME.name)
        val userTracker = quote {
          ctx.query[UserLoginTracker].filter(_.userName == lift(userName))
        }

        val trackerOpt = ctx.run(userTracker.sortBy(_.updatedTime)(Ord.desc)).headOption

        trackerOpt match {
          case Some(tracker) =>
            if (System.currentTimeMillis() - tracker.lockTime < HALF_HOUR) {
              render(400, ActionHelper.msg("You have tried too many times. Please try after 30m later."))
            }
          case None =>
        }

        UserService.login(name, password) match {
          case Some(_) =>
            val token = UUID.randomUUID().toString
            val session = Session(token, Map("userName" -> name))
            UserSessionDB.session.set(name, session)
            List(session)
          case None =>
            val loginTries = ctx.run(
              userTracker.filter(_.createdTime < lift(System.currentTimeMillis() - HALF_HOUR)).size
            )
            if (loginTries == 0) {
              ctx.run(ctx.query[UserLoginTracker].insert(
                _.userName -> lift(userName),
                _.createdTime -> lift(System.currentTimeMillis()),
                _.fails -> lift(1),
                _.updatedTime -> lift(System.currentTimeMillis()),
                _.lockTime -> lift(0l)
              ))
            } else {
              val tracker = ctx.run(userTracker.sortBy(_.updatedTime)(Ord.desc)).head
              if (
                tracker.updatedTime - tracker.createdTime <= FIVE_M &&
                  tracker.fails >= 5
              ) {
                val targetTracker = tracker.copy(fails = tracker.fails + 1, lockTime = System.currentTimeMillis())
                ctx.run(ctx.query[UserLoginTracker].
                  filter(_.id == lift(tracker.id)).
                  update(lift(targetTracker)))
              } else {
                val targetTracker = tracker.copy(fails = tracker.fails + 1, updatedTime = System.currentTimeMillis())
                ctx.run(ctx.query[UserLoginTracker].
                  filter(_.id == lift(tracker.id)).
                  update(lift(targetTracker)))
              }
            }
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
