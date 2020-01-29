package tech.mlsql.app_runtime.plugin

import tech.mlsql.app_runtime.plugin.user.action.{EnableOrDisableQuery, UserQuery, UserReg}
import tech.mlsql.serviceframework.platform.{PluginItem, _}

/**
 * 21/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class PluginDesc extends Plugin {
  override def entries: List[PluginItem] = {
    List(
      PluginItem("userReg", classOf[UserReg].getName, PluginType.action, None),
      PluginItem("users", classOf[UserQuery].getName, PluginType.action, None),
      PluginItem("controlReg", classOf[EnableOrDisableQuery].getName, PluginType.action, None)
    )
  }

  def registerForTest() = {
    val pluginLoader = PluginLoader(Thread.currentThread().getContextClassLoader, this)
    entries.foreach { item =>
      AppRuntimeStore.store.registerAction(item.name, item.clzzName, pluginLoader)
    }
  }
}
