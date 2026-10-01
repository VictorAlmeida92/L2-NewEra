package ext.mods.gameapi.account

object AccountApiValidation {
    private val LOGIN_REGEX = Regex("^[A-Za-z0-9_]{3,45}${'$'}")

    fun validateLogin(login: String?): String? =
        if (login == null || !LOGIN_REGEX.matches(login)) "Login inválido" else null

    fun validatePassword(password: String?): String? =
        if (password == null || password.length !in 8..64) "Senha inválida" else null

    fun validatePasswordPair(current: String?, next: String?): String? {
        validatePassword(current)?.let { return "Senha atual inválida" }
        validatePassword(next)?.let { return "Nova senha inválida" }
        if (current == next) return "A nova senha deve ser diferente da senha atual"
        return null
    }
}
