package tech.mlsql.app_runtime.plugin.user.action

import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.CustomAction
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

class AccessAuth extends CustomAction {

  override def run(params: Map[String, String]): String = {
    val userName = params("userName")
    val resourceName = params("resourceName")
    val canAccess = UserService.accessAuth(userName, resourceName)
    JSONTool.toJsonStr(canAccess)
  }
}

object AccessAuth {
  def action = "accessAuth"

  def plugin = PluginItem(AccessAuth.action,
    classOf[AccessAuth].getName, PluginType.action, None)
}
