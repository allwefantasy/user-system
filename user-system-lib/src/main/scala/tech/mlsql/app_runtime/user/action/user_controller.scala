package tech.mlsql.app_runtime.user.action

import tech.mlsql.app_runtime.user.PluginDB.ctx
import tech.mlsql.app_runtime.user.PluginDB.ctx._
import tech.mlsql.app_runtime.user.Session
import tech.mlsql.app_runtime.user.quill_model.{Role, Team, User, UserRole}
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.ActionContext
import tech.mlsql.serviceframework.platform.form.{Dynamic, FormParams, Input, KV}
import tech.mlsql.serviceframework.platform.{PluginItem, PluginType}


class UserQuery extends BaseAction with ActionInfo {

  override def _run(params: Map[String, String]): String = {
    val items = params.get(UserService.Config.USER_NAME) match {
      case Some(name) => UserService.findUser(name).headOption match {
        case Some(user) => List(user.copy(password = "******"))
        case None => List()
      }
      case None =>
        params.get(UserService.Config.USER_ID) match {
          case Some(id) =>
            ctx.run(ctx.query[User].filter(_.id == lift(id.toInt))).
              map(user => user.copy(password = "******"))
          case None =>
            val context = ActionContext.context()
            val notLogin = JSONTool.parseJson[List[Session]](new IsLoginAction().run(params)).isEmpty
            if (notLogin) {
              render(context.httpContext.response, 400, "login required")
            }
            ctx.run(ctx.query[User]).toList.map(f => f.name)
        }

    }
    JSONTool.toJsonStr(items)
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(UserQuery.Params).toList.reverse)
  }

}


object UserQuery {

  object Params {
    val USER_ID = Input("userId", "")
    val USER_NAME = Input(UserService.Config.USER_NAME, "")
  }

  def action = "users"

  def plugin = PluginItem(UserQuery.action,
    classOf[UserQuery].getName, PluginType.action, None)
}


class AddTeamAction extends BaseAction with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val teamName = params(AddTeamAction.Params.TEAM_NAME.name)
    val userId = getUser(params).get.id
    val size = ctx.run(ctx.query[Team].filter(_.name == lift(teamName)).size)
    if (size != 0) {
      render(400, ActionHelper.msg(s"teamName ${teamName} exists already."))
    }

    ctx.transaction {
      val teamId = ctx.run(ctx.query[Team].insert(_.name -> lift(teamName)).returningGenerated(f => f.id))
      val roleId = ctx.run(ctx.query[Role].insert(_.name -> lift(AddRoleAction.ROLE_ADMIN), _.teamId -> lift(teamId.toInt)).returningGenerated(_.id))
      ctx.run(ctx.query[UserRole].insert(_.roleId -> lift(roleId.toInt), _.userId -> lift(userId)))
    }

    ActionHelper.msg("Success")
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(AddTeamAction.Params).toList.reverse)
  }
}

object AddTeamAction {

  object Params {
    val TEAM_NAME = Input("name", "")
  }

  def action = "/user/team/add"

  def plugin = PluginItem(AddTeamAction.action, classOf[AddTeamAction].getName, PluginType.action, None)
}


class AddUserToRole extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val invitedUserName = params(AddUserToRole.Params.INVITE_USER_NAME.name)
    val teamId = params(AddUserToRole.Params.TEAM_ID.name).toInt
    val roleId = params(AddUserToRole.Params.ROLE_ID.name).toInt

    val targetUser = ctx.run(ctx.query[User].filter(_.name == lift(invitedUserName))).headOption
    targetUser match {
      case Some(user) =>
        val haveTeamIds = ActionHelper.getTeams(getUser(params).get.id)
        if (!haveTeamIds.contains(teamId)) {
          render(400, ActionHelper.msg("No right to access the target Team"))
        }
        val teamRoles = ActionHelper.getRoles(teamId)
        if (!teamRoles.map(_.id).toSet.contains(roleId)) {
          render(400, ActionHelper.msg("No target role"))
        }
        val relations = ctx.run(ctx.query[UserRole].
          filter(item => item.roleId == lift(roleId)).
          filter(_.userId == lift(user.id)))
        if (relations.size == 0) {
          ctx.run(ctx.query[UserRole].insert(_.roleId -> lift(roleId), _.userId -> lift(user.id)))
        }

      case None =>
        render(400, ActionHelper.msg(s"The invited user[${invitedUserName}] is not exists"))
    }
    ActionHelper.msg("Success")
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(AddUserToRole.Params).toList.reverse)
  }
}

object AddUserToRole {

  object Params {
    val INVITE_USER_NAME = Input("invite_user_name", "")
    val TEAM_ID = Dynamic(
      name = "teamId",
      subTpe = "Select",
      depends = List(Params.INVITE_USER_NAME.name),
      valueProviderName = ListTeamForFormAction.action)

    val ROLE_ID = Dynamic(
      name = "roleId",
      subTpe = "Select",
      depends = List(Params.TEAM_ID.name),
      valueProviderName = ListRoleInTeamForFormAction.action
    )
  }

  def action = "/user/role/user/add"

  def plugin = PluginItem(AddUserToRole.action, classOf[AddUserToRole].getName, PluginType.action, None)
}


class RemoveUserFromRole extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val invitedUserName = params(AddUserToRole.Params.INVITE_USER_NAME.name)
    val teamId = params(AddUserToRole.Params.TEAM_ID.name).toInt
    val roleId = params(AddUserToRole.Params.ROLE_ID.name).toInt

    val targetUser = ctx.run(ctx.query[User].filter(_.name == lift(invitedUserName))).headOption
    targetUser match {
      case Some(user) =>
        val haveTeamIds = ActionHelper.getTeams(getUser(params).get.id)
        if (!haveTeamIds.contains(teamId)) {
          render(400, ActionHelper.msg("No right to access the target Team"))
        }
        val teamRoles = ActionHelper.getRoles(teamId)
        if (!teamRoles.map(_.id).toSet.contains(roleId)) {
          render(400, ActionHelper.msg("No target role"))
        }
        val relations = ctx.run(ctx.query[UserRole].
          filter(item => item.roleId == lift(roleId)).
          filter(_.userId == lift(user.id)))
        if (relations.size > 0) {
          ctx.run(ctx.query[UserRole].filter(_.roleId == lift(roleId)).filter(_.userId == lift(user.id)).delete)
        }

      case None =>
        render(400, ActionHelper.msg(s"The user[${invitedUserName}] is not exists"))
    }
    ActionHelper.msg("Success")
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(RemoveUserFromRole.Params).toList.reverse)
  }
}

object RemoveUserFromRole {
  object Params {
    val INVITE_USER_NAME = Input("invite_user_name", "")
    val TEAM_ID = Dynamic(
      name = "teamId",
      subTpe = "Select",
      depends = List(Params.INVITE_USER_NAME.name),
      valueProviderName = ListTeamForFormAction.action)

    val ROLE_ID = Dynamic(
      name = "roleId",
      subTpe = "Select",
      depends = List(Params.TEAM_ID.name),
      valueProviderName = ListRoleInTeamForFormAction.action
    )
  }

  def action = "/usr/role/user/delete"

  def plugin = PluginItem(RemoveUserFromRole.action, classOf[RemoveUserFromRole].getName, PluginType.action, None)
}


class ListRoleInTeamAction extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val userId = getUser(params).get.id
    val teamId = params(ListRoleInTeamAction.Params.TEAM_ID.name)
    val teamIds = ActionHelper.getTeams(userId)
    if (!teamIds.contains(teamId.toInt)) {
      render(400, ActionHelper.msg("No right to access the team."))
    }
    val roles = ActionHelper.getRoles(teamId.toInt)

    JSONTool.toJsonStr(roles)
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(ListRoleInTeamAction.Params).toList.reverse)
  }
}

object ListRoleInTeamAction {
  object Params {
    val TEAM_ID = Input("teamId", "")
  }

  def action = "/user/team/role/list"

  def plugin = PluginItem(ListRoleInTeamAction.action, classOf[ListRoleInTeamAction].getName, PluginType.action, None)
}


class ListRoleInTeamForFormAction extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val roles = JSONTool.parseJson[List[Role]](new ListRoleInTeamAction()._run(params))
    JSONTool.toJsonStr(roles.map { item =>
      KV(Option(item.name), Option(item.id.toString))
    })
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(ListRoleInTeamForFormAction.Params).toList.reverse)
  }
}

object ListRoleInTeamForFormAction {
  object Params {

  }

  def action = "/user/form/team/role/list"

  def plugin = PluginItem(ListRoleInTeamForFormAction.action, classOf[ListRoleInTeamForFormAction].getName, PluginType.action, None)
}


class AddRoleAction extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    // role can only be added by who has role `admin`
    val teamId = params(AddRoleAction.Params.TEAM_ID.name)
    val userId = getUser(params).get.id

    val roles = ctx.run(
      ctx.query[UserRole].
        filter(_.userId == lift(userId)).
        join(ctx.query[Role]).on((userRole, role) => userRole.roleId == role.id).
        map { case (userRole, role) => role }.filter(_.name == lift(AddRoleAction.ROLE_ADMIN))
    )

    val teamIds = roles.map(item => item.teamId).toSet
    if (!teamIds.contains(teamId.toInt)) {
      render(400, ActionHelper.msg(s"No right to add role to target team"))
    }

    val roleName = params(AddRoleAction.Params.ROLE_NAME.name)
    ctx.transaction {
      val roleId = ctx.run(ctx.query[Role].insert(_.name -> lift(roleName), _.teamId -> lift(teamId.toInt)).returningGenerated(_.id))
      ctx.run(ctx.query[UserRole].insert(_.roleId -> lift(roleId.toInt), _.userId -> lift(userId)))
    }

    ActionHelper.msg("Success")
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(AddRoleAction.Params).toList.reverse)
  }
}

object AddRoleAction {

  val ROLE_ADMIN = "admin"

  object Params {
    val ROLE_NAME = Input("name", "")
    val TEAM_ID = Dynamic(
      name = "teamId",
      subTpe = "Select",
      depends = List(Params.ROLE_NAME.name),
      valueProviderName = ListTeamForFormAction.action)
  }

  def action = "/user/role/add"

  def plugin = PluginItem(AddRoleAction.action, classOf[AddRoleAction].getName, PluginType.action, None)
}


class ListTeamForFormAction extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val items = JSONTool.parseJson[List[Team]](new ListTeamAction()._run(params))

    JSONTool.toJsonStr(items.map { item => {
      KV(Option(item.name), Option(item.id.toString))
    }
    })
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(ListTeamForFormAction.Params).toList.reverse)
  }
}

object ListTeamForFormAction {

  object Params {

  }

  def action = "/user/form/team/list"

  def plugin = PluginItem(ListTeamForFormAction.action, classOf[ListTeamForFormAction].getName, PluginType.action, None)
}


class ListRoleAction extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val user = getUser(params).get
    val roles = ctx.run(
      ctx.query[UserRole].filter(_.userId == lift(user.id)).
        join(ctx.query[Role]).on((ur, r) => ur.roleId == r.id)
        map { case (_, role) => role }
    ).toList

    JSONTool.toJsonStr(roles)
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(ListRoleAction.Params).toList.reverse)
  }
}

object ListRoleAction {

  object Params {
    val DESC = Input("desc", "", options = Map("label" -> "Please click the commit"))
  }

  def action = "/user/list/role"

  def plugin = PluginItem(ListRoleAction.action, classOf[ListRoleAction].getName, PluginType.action, None)
}


class ListTeamAction extends ActionRequireLogin with ActionInfo {
  override def _run(params: Map[String, String]): String = {
    val user = getUser(params).get
    val groups = ctx.run(
      ctx.query[UserRole].filter(_.userId == lift(user.id)).
        join(ctx.query[Role]).on((ur, r) => ur.roleId == r.id).
        join(ctx.query[Team]).on { case ((userRole, role), group) => role.teamId == group.id }.
        map { case ((userRole, role), group) => group }
    ).toList

    JSONTool.toJsonStr(groups.groupBy(_.id).map(_._2.head))
  }

  override def _help(): String = {
    JSONTool.toJsonStr(FormParams.toForm(ListTeamAction.Params).toList.reverse)
  }
}

object ListTeamAction {

  object Params {
    val DESC = Input("desc", "", options = Map("label" -> "Please click the commit"))
  }

  def action = "/user/team/list"

  def plugin = PluginItem(ListTeamAction.action, classOf[ListTeamAction].getName, PluginType.action, None)
}

object ActionHelper {
  def msg(str: String) = {
    JSONTool.toJsonStr(Map("msg" -> str))
  }

  def getRoles(teamId: Int): List[Role] = {
    val roles = ctx.run(
      ctx.query[Role].
        filter(_.teamId == lift(teamId))
    ).toList
    roles
  }

  def getTeams(userId: Int) = {

    val roles = ctx.run(
      ctx.query[UserRole].
        filter(_.userId == lift(userId)).
        join(ctx.query[Role]).on((userRole, role) => userRole.roleId == role.id).
        map { case (userRole, role) => role }.filter(_.name == lift(AddRoleAction.ROLE_ADMIN))
    )

    val teamIds = roles.map(item => item.teamId).toSet
    teamIds
  }
}











