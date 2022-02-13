package tech.mlsql.app_runtime.user.quill_model

case class User(id: Int,
                var name: String,
                var password: String,
                activated: Int,
                email: String,
                createdTime: Long
               )

case class Role(id: Int, var name: String, teamId: Int)

case class UserRole(id: Int, var userId: Int, var roleId: Int)

case class Resource(id: Int, var name: String)

case class UserResource(id: Int, var userId: Int, var resourceId: Int)

case class RoleResource(id: Int, var roleId: Int, var resourceId: Int)

case class UserSession(id: Int, name: String, session: String, createTime: Long)


case class Team(id: Int, var name: String)

object UserConstant {
  val ACTIVATED = 1
  val IN_ACTIVATED = 0
}

