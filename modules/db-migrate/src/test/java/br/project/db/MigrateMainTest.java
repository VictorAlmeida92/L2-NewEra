package br.project.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class MigrateMainTest
{
	@Test
	void selectsCanonicalVendorMigrationDirectories()
	{
		assertEquals("database/migrations/postgresql", MigrateMain.selectMigrationsLocation("jdbc:postgresql://db/l2jdb"));
		assertEquals("database/migrations/sqlite", MigrateMain.selectMigrationsLocation("jdbc:sqlite:data/test.db"));
		assertEquals("database/migrations/mariadb", MigrateMain.selectMigrationsLocation("jdbc:mariadb://db/l2jdb"));
		assertEquals("database/migrations/mariadb", MigrateMain.selectMigrationsLocation("jdbc:mysql://db/l2jdb"));
	}

	@Test
	void vendorTreesContainTheSameVersionedMigrations() throws IOException
	{
		final Set<String> postgresql = versionedMigrations("postgresql");
		assertFalse(postgresql.isEmpty());
		assertEquals(postgresql, versionedMigrations("mariadb"));
		assertEquals(postgresql, versionedMigrations("sqlite"));
	}

	@Test
	void legacyMigrationTreeIsNotPartOfTheCanonicalLayout()
	{
		assertFalse(Files.exists(Path.of("db", "migrations")));
		assertTrue(Files.isDirectory(Path.of("database", "seeds")));
		assertTrue(Files.isDirectory(Path.of("database", "fixtures")));
	}

	private static Set<String> versionedMigrations(String vendor) throws IOException
	{
		try (var paths = Files.list(Path.of("database", "migrations", vendor)))
		{
			return paths
				.map(path -> path.getFileName().toString())
				.filter(name -> name.startsWith("V"))
				.map(name -> name.substring(0, name.indexOf("__")))
				.collect(Collectors.toSet());
		}
	}
}
