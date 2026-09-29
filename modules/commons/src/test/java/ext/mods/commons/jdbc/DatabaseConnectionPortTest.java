package ext.mods.commons.jdbc;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.sql.Connection;

import org.junit.jupiter.api.Test;

class DatabaseConnectionPortTest
{
	@Test
	void opensConnectionsThroughTheConfiguredPort() throws Exception
	{
		final Connection expected = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, (proxy, method, args) -> null);
		DatabaseConnection.use(() -> expected);

		try
		{
			assertSame(expected, DatabaseConnection.open());
		}
		finally
		{
			DatabaseConnection.reset();
		}
	}
}
