package ext.mods.gameserver.data.service;

import ext.mods.gameserver.data.PersistenceRegistry;

import java.sql.SQLException;
import java.util.List;

import ext.mods.gameserver.data.repository.OfflineTraderData;
import ext.mods.gameserver.data.repository.OfflineTraderStore;

/** Application boundary for offline trader persistence. */
public final class OfflineTraderPersistenceService
{
	private static final OfflineTraderStore STORE = PersistenceRegistry.offlineTraders();

	private OfflineTraderPersistenceService()
	{
	}

	public static void replaceAll(List<OfflineTraderData> traders) throws SQLException
	{
		STORE.replaceAll(traders);
	}

	public static List<OfflineTraderData> loadAll() throws SQLException
	{
		return STORE.loadAll();
	}

	public static void clear() throws SQLException
	{
		STORE.clear();
	}
}
