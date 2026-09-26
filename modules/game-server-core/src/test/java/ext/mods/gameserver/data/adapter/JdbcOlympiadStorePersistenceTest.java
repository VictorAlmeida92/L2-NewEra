package ext.mods.gameserver.data.adapter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import ext.mods.commons.pool.ConnectionPool;

/** Optional PostgreSQL contract test for the Olympiad persistence adapter. */
class JdbcOlympiadStorePersistenceTest
{
	@AfterEach
	void tearDown()
	{
		ConnectionPool.shutdown();
	}

	@Test
	void loadsOlympiadStateAndRankingsThroughTheAdapter()
	{
		Assumptions.assumeTrue(databaseUrl() != null, "Set DB_TEST_URL to run the Olympiad persistence contract test");
		ConnectionPool.init(databaseUrl(), databaseUser(), databasePassword(), "OlympiadStoreContractPool");

		final JdbcOlympiadStore store = new JdbcOlympiadStore();
		assertNotNull(store.loadStatus());
		assertNotNull(store.loadNobles());
		assertNotNull(store.loadRankedNobleIds(5));
		assertNotNull(store.loadClassLeaders(88, 5, true));
		assertNotNull(store.loadClassLeaders(88, 5, false));
		assertNotNull(store.loadLastNoblePoints(Integer.MAX_VALUE));
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
