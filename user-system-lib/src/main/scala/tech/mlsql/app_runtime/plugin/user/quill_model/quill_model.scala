package tech.mlsql.app_runtime.plugin.user.quill_model

case class User(id: Int, var name: String, var password: String)

case class Role(id: Int, var name: String)

case class UserRole(id: Int, var userId: Int, var roleId: Int)

case class Resource(id: Int, var name: Int)

case class UserResource(id: Int, var userId: Int, var resourceId: Int)

case class RoleResource(id: Int, var roleId: Int, var resourceId: Int)

case class UserSession(id: Int, name: String, session: String)