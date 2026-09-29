package ext.mods.gameserver.network.serverpackets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class NpcHtmlMessageTest
{
	@Test
	void postProcessingWithoutAttachedClientDoesNotThrow()
	{
		final NpcHtmlMessage message = new NpcHtmlMessage(0);
		message.setHtml("<html><body>test</body></html>");

		assertDoesNotThrow(message::runImpl);
	}
}
