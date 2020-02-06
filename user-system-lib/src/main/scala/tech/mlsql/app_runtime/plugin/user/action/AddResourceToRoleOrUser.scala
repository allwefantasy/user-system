package tech.mlsql.app_runtime.plugin.user.action

import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx._
import tech.mlsql.app_runtime.plugin.user.action.AddResourceToRoleOrUser.Params
import tech.mlsql.app_runtime.plugin.user.quill_model.{Resource, Role, RoleResource, UserResource}
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.CustomAction
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 4/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class AddResourceToRoleOrUser extends CustomAction {
  override def run(params: Map[String, String]): String = {
    val resourceName = params(Params.RESOURCE_NAME)
    val resourceOpt = ctx.run(
      ctx.query[Resource].filter(_.name == lift(resourceName))).headOption

    val resourceId = resourceOpt match {
      case Some(r) => r.id
      case None =>
        ctx.run(ctx.query[Resource].insert(_.name -> lift(resourceName)).returningGenerated(_.id))
    }

    params.get(Params.ROLE_NAME) match {
      case Some(roleName) =>
        val roleId = ctx.run(ctx.query[Role].filter(_.name == lift(roleName))).head.id
        ctx.run(ctx.query[RoleResource].
          insert(_.resourceId -> lift(resourceId), _.roleId -> lift(roleId)).
          onConflictIgnore(_.roleId, _.resourceId))
      case None =>
        val userId = UserService.findUser(params(Params.AUTHORIZED_USER_NAME)).head.id
        ctx.run(ctx.query[UserResource].
          insert(_.resourceId -> lift(resourceId), _.userId -> lift(userId)).
          onConflictIgnore(_.userId, _.resourceId))
    }
    JSONTool.toJsonStr(Map("msg" -> "success"))
  }
}

object AddResourceToRoleOrUser {

  object Params {
    val ROLE_NAME = "roleName"
    val AUTHORIZED_USER_NAME = "authUser"
    val RESOURCE_NAME = "resourceName"
  }

  def action = "addResourceAuth"

  def plugin = PluginItem(AddResourceToRoleOrUser.action,
    classOf[AddResourceToRoleOrUser].getName, PluginType.action, None)
}
