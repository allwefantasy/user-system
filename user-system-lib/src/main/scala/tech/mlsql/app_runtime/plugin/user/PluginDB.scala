package tech.mlsql.app_runtime.plugin.user

import net.csdn.jpa.QuillDB
import tech.mlsql.app_runtime.db.quill_model.DictType
import tech.mlsql.app_runtime.db.service.BasicDBService

/**
 * 28/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
object PluginDB {
  val plugin_name = "user-system"
  lazy val ctx = {
    val dbName = BasicDBService.fetch(plugin_name, DictType.INSTANCE_TO_DB)
    val dbInfo = dbName.getOrElse {
      throw new RuntimeException(s"DB: cannot init db for plugin ${plugin_name} ")
    }
    val dbConfig = BasicDBService.fetchDB(dbInfo.value).getOrElse {
      throw new RuntimeException(s"DB: cannot get db config for plugin ${plugin_name} ")
    }
    QuillDB.createNewCtxByNameFromStr(dbConfig.name, dbConfig.value)
  }
}
