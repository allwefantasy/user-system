package tech.mlsql.app_runtime.user

object SystemConfig extends Enumeration {
  type SystemConfig = Value
  val REG_ENABLE = Value("enable")
  val REG_DISABLE = Value("disable")
  val REG_KEY = Value("userReg")
}
