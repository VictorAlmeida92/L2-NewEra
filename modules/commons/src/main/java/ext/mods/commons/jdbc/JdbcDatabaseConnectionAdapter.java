package ext.mods.commons.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import ext.mods.commons.pool.ConnectionPool;

/** Default JDBC adapter for the application database connection port. */
public final class JdbcDatabaseConnectionAdapter implements DatabaseConnectionPort
{
	@Override
	public Connection open() throws SQLException
	{
		return ConnectionPool.getConnection();
	}
}
