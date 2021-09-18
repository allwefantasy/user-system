package tech.mlsql.app_runtime.user

/**
 * __redis__user-system -> boolean
 * __redis__config_user-system ->
 */
object RedisDB {
  val SWITCH_KEY = s"__redis__${PluginDB.plugin_name}"
  val CONFIG_KEY = s"__redis__config_${PluginDB.plugin_name}"

}
