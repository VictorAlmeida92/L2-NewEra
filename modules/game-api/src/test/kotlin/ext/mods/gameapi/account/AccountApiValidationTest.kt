package ext.mods.gameapi.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccountApiValidationTest {
    @Test
    fun `accepts game account credentials`() {
        assertNull(AccountApiValidation.validateLogin("MagicArrow_01"))
        assertNull(AccountApiValidation.validatePassword("1234567890"))
    }

    @Test
    fun `rejects credentials outside login server contract`() {
        assertEquals("Login inválido", AccountApiValidation.validateLogin("a"))
        assertEquals("Login inválido", AccountApiValidation.validateLogin("has space"))
        assertEquals("Senha inválida", AccountApiValidation.validatePassword("short"))
    }

    @Test
    fun `requires a different password when changing it`() {
        assertEquals(
            "A nova senha deve ser diferente da senha atual",
            AccountApiValidation.validatePasswordPair("1234567890", "1234567890")
        )
        assertNull(AccountApiValidation.validatePasswordPair("1234567890", "0987654321"))
    }
}
