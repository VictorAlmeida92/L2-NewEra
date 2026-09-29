package ext.mods.gameserver.data.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ext.mods.gameserver.data.repository.BufferSchemeStore;
import ext.mods.config.ConfigNpcs;

class BufferManagerPersistenceTest
{
	@Test
	void customSchemeMutationsArePersistedImmediately()
	{
		ConfigNpcs.BUFFER_MAX_SCHEMES = 4;
		final RecordingStore store = new RecordingStore();
		final BufferManager manager = new BufferManager(store, false);

		assertTrue(manager.setScheme(9001, "Varkas", new ArrayList<>()));
		assertEquals(List.of(new BufferSchemeStore.SchemeRecord(9001, "Varkas", "", "")), store.lastSaved);

		assertTrue(manager.deleteScheme(9001, "Varkas"));
		assertTrue(store.lastSaved.isEmpty());
	}

	private static final class RecordingStore implements BufferSchemeStore
	{
		private List<SchemeRecord> lastSaved = List.of();

		@Override
		public List<SchemeRecord> loadSchemes()
		{
			return List.of();
		}

		@Override
		public void replaceSchemes(List<SchemeRecord> schemes)
		{
			lastSaved = List.copyOf(schemes);
		}
	}
}
