package ext.mods.gameserver.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class PersistenceRegistryTest
{
	@Test
	void exposesStableRepositoryPorts()
	{
		assertSame(PersistenceRegistry.bookmarks(), PersistenceRegistry.bookmarks());
		assertSame(PersistenceRegistry.bufferSchemes(), PersistenceRegistry.bufferSchemes());
		assertSame(PersistenceRegistry.buyLists(), PersistenceRegistry.buyLists());
		assertSame(PersistenceRegistry.castles(), PersistenceRegistry.castles());
		assertSame(PersistenceRegistry.characterSelection(), PersistenceRegistry.characterSelection());
		assertSame(PersistenceRegistry.characters(), PersistenceRegistry.characters());
		assertSame(PersistenceRegistry.clanHallAuctions(), PersistenceRegistry.clanHallAuctions());
		assertSame(PersistenceRegistry.clans(), PersistenceRegistry.clans());
		assertSame(PersistenceRegistry.customEvents(), PersistenceRegistry.customEvents());
		assertSame(PersistenceRegistry.gameServerRegistration(), PersistenceRegistry.gameServerRegistration());
		assertSame(PersistenceRegistry.items(), PersistenceRegistry.items());
		assertSame(PersistenceRegistry.offlineTraders(), PersistenceRegistry.offlineTraders());
		assertSame(PersistenceRegistry.olympiad(), PersistenceRegistry.olympiad());
		assertSame(PersistenceRegistry.playerAuxiliary(), PersistenceRegistry.playerAuxiliary());
		assertSame(PersistenceRegistry.playerInfo(), PersistenceRegistry.playerInfo());
		assertSame(PersistenceRegistry.premium(), PersistenceRegistry.premium());
		assertSame(PersistenceRegistry.quests(), PersistenceRegistry.quests());
		assertSame(PersistenceRegistry.recommendations(), PersistenceRegistry.recommendations());
		assertSame(PersistenceRegistry.serverMemos(), PersistenceRegistry.serverMemos());
		assertSame(PersistenceRegistry.skills(), PersistenceRegistry.skills());
		assertSame(PersistenceRegistry.subclasses(), PersistenceRegistry.subclasses());
	}

	@Test
	void exposesEveryPersistencePort()
	{
		assertNotNull(PersistenceRegistry.bookmarks());
		assertNotNull(PersistenceRegistry.bufferSchemes());
		assertNotNull(PersistenceRegistry.buyLists());
		assertNotNull(PersistenceRegistry.castles());
		assertNotNull(PersistenceRegistry.characterSelection());
		assertNotNull(PersistenceRegistry.characters());
		assertNotNull(PersistenceRegistry.clanHallAuctions());
		assertNotNull(PersistenceRegistry.clans());
		assertNotNull(PersistenceRegistry.customEvents());
		assertNotNull(PersistenceRegistry.gameServerRegistration());
		assertNotNull(PersistenceRegistry.items());
		assertNotNull(PersistenceRegistry.offlineTraders());
		assertNotNull(PersistenceRegistry.olympiad());
		assertNotNull(PersistenceRegistry.playerAuxiliary());
		assertNotNull(PersistenceRegistry.playerInfo());
		assertNotNull(PersistenceRegistry.premium());
		assertNotNull(PersistenceRegistry.quests());
		assertNotNull(PersistenceRegistry.recommendations());
		assertNotNull(PersistenceRegistry.serverMemos());
		assertNotNull(PersistenceRegistry.skills());
		assertNotNull(PersistenceRegistry.subclasses());
	}
}
