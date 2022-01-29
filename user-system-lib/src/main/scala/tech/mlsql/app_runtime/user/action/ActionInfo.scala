package tech.mlsql.app_runtime.user.action

import tech.mlsql.serviceframework.platform.action.attribute.{GroupAttribute, ModuleAttribute}

/**
 * 29/1/2022 WilliamZhu(allwefantasy@gmail.com)
 */
trait ActionInfo extends GroupAttribute with ModuleAttribute{
  override def groupName(): String = {
    "User"
  }

  override def moduleName(): String = {
    "user"
  }
}
