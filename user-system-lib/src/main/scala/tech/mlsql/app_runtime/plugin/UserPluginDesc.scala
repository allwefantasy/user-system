package tech.mlsql.app_runtime.plugin

import tech.mlsql.app_runtime.user.action._
import tech.mlsql.serviceframework.platform.{Plugin, PluginItem}

class UserPluginDesc extends Plugin {
  override def entries: List[PluginItem] = {
    List(
      UserLoginAction.plugin,
      UserLogOutAction.plugin,
      UserQuery.plugin,
      EnableOrDisableRegAction.plugin,
      UserRegAction.plugin,
      IsLoginAction.plugin,
      AccessAuth.plugin,
      CheckAuthAction.plugin,
      AddResourceToRoleOrUser.plugin
    )
  }
}
