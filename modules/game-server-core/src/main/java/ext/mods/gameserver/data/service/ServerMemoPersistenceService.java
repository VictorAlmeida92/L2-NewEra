package ext.mods.gameserver.data.service;

import ext.mods.gameserver.data.PersistenceRegistry;

import java.sql.SQLException;
import java.util.Map;

import ext.mods.gameserver.data.repository.ServerMemoStore;

/** Application boundary for global server memo persistence. */
public final class ServerMemoPersistenceService
{
	private static final ServerMemoStore STORE = PersistenceRegistry.serverMemos();

	private ServerMemoPersistenceService()
	{
	}

	public static Map<String, String> load() throws SQLException
	{
		return STORE.load();
	}

	public static void upsert(String key, String value) throws SQLException
	{
		STORE.upsert(key, value);
	}

	public static void delete(String key) throws SQLException
	{
		STORE.delete(key);
	}
}
