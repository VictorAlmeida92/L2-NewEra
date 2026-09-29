package ext.mods.gameserver.data.service;

import ext.mods.gameserver.data.PersistenceRegistry;

import java.sql.SQLException;
import java.util.List;

import ext.mods.gameserver.data.repository.BookmarkStore;
import ext.mods.gameserver.model.records.Bookmark;

/** Application boundary for player bookmark persistence. */
public final class BookmarkPersistenceService
{
	private static final BookmarkStore STORE = PersistenceRegistry.bookmarks();

	private BookmarkPersistenceService()
	{
	}

	public static List<Bookmark> load() throws SQLException
	{
		return STORE.load();
	}

	public static void save(Bookmark bookmark) throws SQLException
	{
		STORE.save(bookmark);
	}

	public static void delete(String name, int objectId) throws SQLException
	{
		STORE.delete(name, objectId);
	}
}
