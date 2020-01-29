package tech.mlsql.app_runtime.plugin

import net.csdn.ServiceFramwork
import net.csdn.bootstrap.Application
import tech.mlsql.app_runtime.db.action.{AddDBAction, LoadDBAction}
import tech.mlsql.serviceframework.platform.{AppRuntimeStore, PluginItem, PluginLoader, PluginType}

object App {
  def main(args: Array[String]): Unit = {
    val applicationYamlName = "application.yml"
    ServiceFramwork.applicaionYamlName(applicationYamlName)
    ServiceFramwork.scanService.setLoader(classOf[App])
    ServiceFramwork.enableNoThreadJoin()

    val plugin = new PluginDesc

    // register db plugin first
    val pluginLoader = PluginLoader(Thread.currentThread().getContextClassLoader, plugin)
    List(
      PluginItem("addDB", classOf[AddDBAction].getName, PluginType.action, None),
      PluginItem("loadDB", classOf[LoadDBAction].getName, PluginType.action, None)
    ).foreach { item =>
      AppRuntimeStore.store.registerAction(item.name, item.clzzName, pluginLoader)
    }
    plugin.registerForTest()

    Application.main(args)
    Thread.currentThread().join()

  }

}

class App {

}
