package tech.mlsql.app_runtime.user.action

import tech.mlsql.serviceframework.platform.form.{FormParams, Input}
import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.action.AddResourceToRoleOrUser.Params
import tech.mlsql.app_runtime.user.quill_model.{Resource, Role, RoleResource, UserResource}
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 4/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class AddResourceToRoleOrUser extends ActionRequireLogin {


  override def _run(params: Map[String, String]): String = {
    val canAccess = UserService.checkLoginAndResourceAccess(AddResourceToRoleOrUser.action, params)
    if (!canAccess.access) {
      return render(400, JSONTool.toJsonStr(List(Map("msg" -> canAccess.msg))))
    }
    val resourceName = params(Params.RESOURCE_NAME.name)
    val resourceOpt = ctx.run(
      ctx.query[Resource].filter(_.name == lift(resourceName))).headOption

    val resourceId = resourceOpt match {
      case Some(r) => r.id
      case None =>
        ctx.run(ctx.query[Resource].insert(_.name -> lift(resourceName)).returningGenerated(_.id))
    }

    params.get(Params.ROLE_NAME.name) match {
      case Some(roleName) =>
        val roleId = ctx.run(ctx.query[Role].filter(_.name == lift(roleName))).head.id
        ctx.run(ctx.query[RoleResource].
          insert(_.resourceId -> lift(resourceId), _.roleId -> lift(roleId)).
          onConflictIgnore(_.roleId, _.resourceId))
      case None =>
        val userId = UserService.findUser(params(Params.AUTHORIZED_USER_NAME.name)).head.id
        ctx.run(ctx.query[UserResource].
          insert(_.resourceId -> lift(resourceId), _.userId -> lift(userId)).
          onConflictIgnore(_.userId, _.resourceId))
    }
    JSONTool.toJsonStr(Map("msg" -> "success"))
  }

  override def _help(): String = JSONTool.toJsonStr(
    FormParams.toForm(AddResourceToRoleOrUser.Params).toList.reverse)
}

object AddResourceToRoleOrUser {

  object Params {
    val ADMIN_TOKEN = Input("admin_token", "")
    val ROLE_NAME = Input("roleName", "")
    val AUTHORIZED_USER_NAME = Input("authUser", "")
    val RESOURCE_NAME = Input("resourceName", "")
  }

  def action = "addResourceAuth"

  def plugin = PluginItem(AddResourceToRoleOrUser.action,
    classOf[AddResourceToRoleOrUser].getName, PluginType.action, None)
}
