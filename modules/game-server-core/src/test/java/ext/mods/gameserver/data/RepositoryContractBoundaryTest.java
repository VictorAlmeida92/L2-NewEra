package ext.mods.gameserver.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class RepositoryContractBoundaryTest
{
	@Test
	void repositoryPackageExistsAndContainsNoConcreteJdbcOrSql()
	{
		final Path repositoryPackage = findRepositoryPackage();
		assertTrue(Files.isDirectory(repositoryPackage));

		try (Stream<Path> files = Files.list(repositoryPackage))
		{
			files.filter(path -> path.getFileName().toString().endsWith("Store.java"))
				.forEach(this::assertPortHasNoConcreteDatabaseDetails);
		}
		catch (IOException e)
		{
			throw new AssertionError("Unable to inspect repository contracts", e);
		}
	}

	private Path findRepositoryPackage()
	{
		Path root = Path.of("").toAbsolutePath();
		while (root != null)
		{
			final Path candidate = root.resolve("modules/game-server-core/src/main/java/ext/mods/gameserver/data/repository");
			if (Files.isDirectory(candidate))
				return candidate;
			root = root.getParent();
		}
		throw new AssertionError("Unable to locate the repository package from the test working directory");
	}

	private void assertPortHasNoConcreteDatabaseDetails(Path path)
	{
		try
		{
			final String source = Files.readString(path);
			assertFalse(source.contains("Connection"), path + " must not expose JDBC connections");
			assertFalse(source.contains("PreparedStatement"), path + " must not expose JDBC statements");
			assertFalse(source.contains("ResultSet"), path + " must not expose JDBC result sets");
			assertFalse(source.contains("SELECT "), path + " must not contain SQL");
			assertFalse(source.contains("INSERT "), path + " must not contain SQL");
			assertFalse(source.contains("UPDATE "), path + " must not contain SQL");
			assertFalse(source.contains("DELETE "), path + " must not contain SQL");
		}
		catch (IOException e)
		{
			throw new AssertionError("Unable to inspect " + path, e);
		}
	}
}
