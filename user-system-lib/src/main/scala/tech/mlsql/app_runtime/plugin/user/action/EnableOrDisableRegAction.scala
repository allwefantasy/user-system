package tech.mlsql.app_runtime.plugin.user.action

import tech.mlsql.app_runtime.commons.{FormParams, Input, KV, Select}
import tech.mlsql.app_runtime.db.quill_model.DictType
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.app_runtime.plugin.user.SystemConfig
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}

/**
 * 19/2/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class EnableOrDisableRegAction extends ActionRequireResourceAccess {
  override def _run(params: Map[String, String]): String = {
    val value = if (params(EnableOrDisableRegAction.Params.ENABLE_REG.name).toBoolean) SystemConfig.REG_ENABLE.toString
    else SystemConfig.REG_DISABLE.toString
    BasicDBService.addItem(SystemConfig.REG_KEY.toString, value, DictType.SYSTEM_CONFIG)
    JSONTool.toJsonStr(Map("msg" -> "ok"))
  }

  override def _help(): String = {
    JSONTool.toJsonStr(
      FormParams.toForm(EnableOrDisableRegAction.Params).toList.reverse)
  }
}

object EnableOrDisableRegAction {

  object Params {
    val ADMIN_TOKEN = Input("admin_token", "")
    
    val ENABLE_REG = Select("enable", values = List(), valueProvider = Option(() => {
      List(KV(Option("Enable"), Option("true")), KV(Option("Disable"), Option("false")))
    }))
  }

  def action = "controlReg"

  def plugin = PluginItem(EnableOrDisableRegAction.action,
    classOf[EnableOrDisableRegAction].getName, PluginType.action, None)
}
