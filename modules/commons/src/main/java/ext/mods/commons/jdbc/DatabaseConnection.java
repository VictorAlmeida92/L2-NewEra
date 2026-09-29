package ext.mods.commons.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Composition root for the database connection port.
 *
 * <p>The default is the production JDBC adapter. Tests and future database
 * adapters may replace it before bootstrapping a module. Existing callers
 * still own and close the returned connection, preserving the current
 * try-with-resources and transaction behavior.</p>
 */
public final class DatabaseConnection
{
	private static volatile DatabaseConnectionPort PORT = new JdbcDatabaseConnectionAdapter();

	private DatabaseConnection()
	{
	}

	public static Connection open() throws SQLException
	{
		return PORT.open();
	}

	public static void use(DatabaseConnectionPort port)
	{
		PORT = Objects.requireNonNull(port, "port");
	}

	public static void reset()
	{
		PORT = new JdbcDatabaseConnectionAdapter();
	}
}
