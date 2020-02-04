package tech.mlsql.app_runtime.plugin.user.action

import tech.mlsql.app_runtime.db.action.BasicActionProxy
import tech.mlsql.app_runtime.plugin.user.PluginDB

object UserSystemActionProxy {
  lazy val proxy = new BasicActionProxy(PluginDB.plugin_name)
}
