package tech.mlsql.app_runtime.user.action

import tech.mlsql.app_runtime.db.action.BasicActionProxy
import tech.mlsql.app_runtime.user.PluginDB

object UserSystemActionProxy {
  lazy val proxy = new BasicActionProxy(PluginDB.plugin_name)
}
