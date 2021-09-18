package tech.mlsql.app_runtime.user.action

import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.CustomAction
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 4/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class CheckAuthAction extends CustomAction {
  override def run(params: Map[String, String]): String = {
    val access = UserService.checkLoginAndResourceAccess(params(UserService.Config.RESOURCE_KEY), params)
    JSONTool.toJsonStr(access)
  }
}

object CheckAuthAction {
  def action = "checkAuth"

  def plugin = PluginItem(CheckAuthAction.action,
    classOf[CheckAuthAction].getName, PluginType.action, None)
}
