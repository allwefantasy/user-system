package tech.mlsql.app_runtime.user.action

import tech.mlsql.app_runtime.db.quill_model.DictType
import tech.mlsql.app_runtime.db.service.BasicDBService
import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.{Session, SystemConfig}
import tech.mlsql.app_runtime.user.quill_model.{Resource, RoleResource, User, UserResource, UserRole}
import tech.mlsql.common.utils.Md5
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.RenderFunctions


object UserService extends RenderFunctions {

  object Config {
    val LOGIN_TOKEN = "access-token"
    val USER_NAME = "userName"
    val USER_ID = "userId"
    val RESOURCE_KEY = "resourceKey"
  }


  def isLogin(userName: String, token: String) = {
    val isLoginStr = new IsLoginAction().run(Map(
      UserService.Config.USER_NAME -> userName,
      UserService.Config.LOGIN_TOKEN -> token
    ))
    JSONTool.parseJson[List[Session]](isLoginStr)
  }

  def checkLoginAndResourceAccess(resourceKey: String, params: Map[String, String]): CanAccess = {
    var canAccess = false

    val token = params.getOrElse("admin_token", "")
    if (BasicDBService.adminToken == token) return CanAccess(true, "")

    if (BasicDBService.isDBSupport) {
      val userName = if (params.contains(UserService.Config.USER_ID)) {
        val users = JSONTool.parseJson[List[User]](new UserQuery().run(params))
        users.head.name
      } else {
        params.getOrElse(UserService.Config.USER_NAME, "")
      }

      val token = params.getOrElse(UserService.Config.LOGIN_TOKEN, "")
      if (isLogin(userName, token).isEmpty) {
        return CanAccess(false, "login is required")
      }

      val resourceName = resourceKey
      val resStr = new AccessAuth().run(
        Map(
          "userName" -> userName,
          "resourceName" -> resourceName
        )
      )
      val res = JSONTool.parseJson[CanAccess](resStr)
      canAccess = res.access
    }
    if (canAccess) CanAccess(canAccess, "")
    else CanAccess(canAccess, "No right to access this resource")

  }


  def accessAuth(userName: String, resourceName: String): CanAccess = {
    // check the people resource
    val userId = findUser(userName).head.id
    val resourceOpt = ctx.run(ctx.query[Resource].filter(_.name == lift(resourceName))).headOption

    if (resourceOpt.isEmpty) return CanAccess(false, s"${resourceName} is not exists")
    val resourceId = resourceOpt.get.id

    val canAccess = ctx.run(ctx.query[UserResource].
      filter(f => f.userId == lift(userId) && f.resourceId == lift(resourceId))).
      headOption.isDefined
    if (canAccess) return CanAccess(true, "")

    // check the role the people belongs
    val roles = ctx.run(ctx.query[UserRole].filter { ur =>
      ur.userId == lift(userId)
    }).toList

    if (roles.size == 0) return CanAccess(false, "")

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
    return CanAccess(roleCanAccess, "")
  }

  def isEnableReg = {
    BasicDBService.fetch(SystemConfig.REG_KEY.toString, DictType.SYSTEM_CONFIG).headOption match {
      case Some(item) => item.value == SystemConfig.REG_ENABLE.toString
      case None => false
    }
  }

  def login(name: String, password: String) = {
    ctx.run(users().
      filter(f => f.name == lift(name) && f.password == lift(Md5.md5Hash(password)))).
      headOption
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

case class CanAccess(access: Boolean, msg: String)
