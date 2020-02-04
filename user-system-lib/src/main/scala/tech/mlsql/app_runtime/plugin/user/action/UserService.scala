package tech.mlsql.app_runtime.plugin.user.action

import tech.mlsql.app_runtime.db.quill_model.DictType
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx
import tech.mlsql.app_runtime.plugin.user.PluginDB.ctx._
import tech.mlsql.app_runtime.plugin.user.SystemConfig
import tech.mlsql.app_runtime.plugin.user.quill_model._
import tech.mlsql.common.utils.Md5


object UserService {

  def accessAuth(userName: String, resourceName: String): CanAccess = {
    // check the people resource
    val userId = findUser(userName).head.id
    val resourceId = ctx.run(ctx.query[Resource].filter(_.name == lift(resourceName))).head.id
    val canAccess = ctx.run(ctx.query[UserResource].
      filter(f => f.userId == lift(userId) && f.resourceId == lift(resourceId))).
      headOption.isDefined
    if (canAccess) return CanAccess(true)

    // check the role the people belongs
    val roles = ctx.run(ctx.query[UserRole].filter { ur =>
      ur.userId == lift(userId)
    }).toList

    if (roles.size == 0) return CanAccess(false)

    var roleCanAccess = false

    roles.foreach { role =>
      if (!roleCanAccess) {
        val canAccess = ctx.run(ctx.query[RoleResource].filter { rr =>
          rr.roleId == lift(role.roleId) && rr.resourceId == lift(resourceId)
        }).headOption.isDefined

        if (canAccess) {
          roleCanAccess = canAccess
        }
      }

    }
    return CanAccess(roleCanAccess)
  }

  def isEnableReg = {
    BasicDBService.fetch(SystemConfig.REG_KEY.toString, DictType.SYSTEM_CONFIG).headOption match {
      case Some(item) => item.value == SystemConfig.REG_ENABLE.toString
      case None => false
    }
  }

  def login(name: String, password: String) = {
    ctx.run(users().filter(f => f.name == lift(name) && f.password == lift(Md5.md5Hash(password)))).headOption
  }

  def findUser(name: String) = {
    ctx.run(users().filter(_.name == lift(name))).headOption
  }

  def users() = {
    quote {
      ctx.query[User]
    }
  }

}

case class CanAccess(access: Boolean)
