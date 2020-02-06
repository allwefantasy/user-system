package tech.mlsql.app_runtime.plugin

import tech.mlsql.app_runtime.plugin.user.action._
import tech.mlsql.serviceframework.platform.{PluginItem, _}

/**
 * 21/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class PluginDesc extends Plugin {
  override def entries: List[PluginItem] = {
    List(
      UserReg.plugin,
      UserQuery.plugin,
      EnableOrDisableQuery.plugin,
      UserLogin.plugin,
      IsLogin.plugin,
      AccessAuth.plugin,
      CheckAuthAction.plugin,
      AddResourceToRoleOrUser.plugin
    )
  }

  def registerForTest() = {
    val pluginLoader = PluginLoader(Thread.currentThread().getContextClassLoader, this)
    entries.foreach { item =>
      AppRuntimeStore.store.registerAction(item.name, item.clzzName, pluginLoader)
    }
  }
}
