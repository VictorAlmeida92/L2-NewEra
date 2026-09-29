package ext.mods.commons.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Application port used by persistence flows to open a database session.
 *
 * <p>The game and login modules depend on this port instead of selecting a
 * concrete connection pool. The default adapter remains JDBC so this change
 * does not alter the database protocol or transaction semantics.</p>
 */
@FunctionalInterface
public interface DatabaseConnectionPort
{
	Connection open() throws SQLException;
}
