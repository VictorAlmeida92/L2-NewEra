package ext.mods.gameserver.data.service;

import ext.mods.gameserver.data.PersistenceRegistry;

import java.sql.SQLException;
import java.util.List;

import ext.mods.gameserver.data.repository.PlayerDirectoryEntry;
import ext.mods.gameserver.data.repository.PlayerInfoStore;

/** Application boundary for the character directory persistence. */
public final class PlayerInfoPersistenceService
{
	private static final PlayerInfoStore STORE = PersistenceRegistry.playerInfo();

	private PlayerInfoPersistenceService()
	{
	}

	public static List<PlayerDirectoryEntry> load() throws SQLException
	{
		return STORE.load();
	}
}
