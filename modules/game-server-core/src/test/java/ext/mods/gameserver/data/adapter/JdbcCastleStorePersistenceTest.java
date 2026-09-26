package ext.mods.gameserver.data.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import ext.mods.commons.pool.ConnectionPool;

/** Optional PostgreSQL contract test for the castle persistence adapter. */
class JdbcCastleStorePersistenceTest
{
	@AfterEach
	void tearDown()
	{
		ConnectionPool.shutdown();
	}

	@Test
	void loadsCastleStateAndUpgradeTablesThroughTheAdapter()
	{
		Assumptions.assumeTrue(databaseUrl() != null, "Set DB_TEST_URL to run the castle persistence contract test");
		ConnectionPool.init(databaseUrl(), databaseUser(), databasePassword(), "CastleStoreContractPool");

		final JdbcCastleStore store = new JdbcCastleStore();
		final var castles = store.loadCastles();
		assertFalse(castles.isEmpty(), "The baseline should contain castle definitions");

		final int castleId = castles.get(0).id();
		assertNotNull(store.loadOwnerClanIds(castleId));
		assertNotNull(store.loadTrapUpgrades(castleId));
		assertNotNull(store.loadDoorUpgrades(castleId));
	}

	private static String databaseUrl()
	{
		return firstNonBlank(System.getProperty("dbTestUrl"), System.getenv("DB_TEST_URL"));
	}

	private static String databaseUser()
	{
		return firstNonBlank(System.getProperty("dbTestUser"), System.getenv("DB_TEST_USER"), "brproject");
	}

	private static String databasePassword()
	{
		return firstNonBlank(System.getProperty("dbTestPassword"), System.getenv("DB_TEST_PASSWORD"), "brproject");
	}

	private static String firstNonBlank(String... values)
	{
		for (String value : values)
			if (value != null && !value.isBlank())
				return value;
		return null;
	}
}
