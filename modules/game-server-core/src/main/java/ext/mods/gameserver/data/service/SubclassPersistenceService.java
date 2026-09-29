package ext.mods.gameserver.data.service;

import ext.mods.gameserver.data.PersistenceRegistry;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import ext.mods.gameserver.data.repository.SubclassRecord;
import ext.mods.gameserver.data.repository.SubclassStore;

/** Application boundary for player subclass persistence. */
public final class SubclassPersistenceService
{
	private static final SubclassStore STORE = PersistenceRegistry.subclasses();

	private SubclassPersistenceService()
	{
	}

	public static List<SubclassRecord> load(int characterObjectId) throws SQLException
	{
		return STORE.load(characterObjectId);
	}

	public static void update(int characterObjectId, Collection<SubclassRecord> subclasses) throws SQLException
	{
		STORE.update(characterObjectId, subclasses);
	}

	public static void add(int characterObjectId, SubclassRecord subclass) throws SQLException
	{
		STORE.add(characterObjectId, subclass);
	}

	public static void wipe(int characterObjectId, int classIndex) throws SQLException
	{
		STORE.wipe(characterObjectId, classIndex);
	}
}
