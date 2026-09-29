package ext.mods.commons.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ext.mods.commons.jdbc.DatabaseConnection;

class JdbcSupportTransactionTest
{
	@AfterEach
	void resetDatabaseConnectionPort()
	{
		DatabaseConnection.reset();
	}

	@Test
	void commitsAndRestoresAutoCommitAfterSuccessfulWork() throws Exception
	{
		final AtomicBoolean autoCommit = new AtomicBoolean(true);
		final AtomicInteger commits = new AtomicInteger();
		final AtomicInteger rollbacks = new AtomicInteger();
		final Connection connection = connection(autoCommit, commits, rollbacks);
		DatabaseConnection.use(() -> connection);

		JdbcSupport.transaction(con -> assertSame(connection, con));

		assertEquals(1, commits.get());
		assertEquals(0, rollbacks.get());
		assertEquals(true, autoCommit.get());
	}

	@Test
	void rollsBackAndRestoresAutoCommitAfterFailure()
	{
		final AtomicBoolean autoCommit = new AtomicBoolean(true);
		final AtomicInteger commits = new AtomicInteger();
		final AtomicInteger rollbacks = new AtomicInteger();
		DatabaseConnection.use(() -> connection(autoCommit, commits, rollbacks));

		assertThrows(IllegalStateException.class, () -> JdbcSupport.transaction(con -> {
			throw new IllegalStateException("expected failure");
		}));

		assertEquals(0, commits.get());
		assertEquals(1, rollbacks.get());
		assertEquals(true, autoCommit.get());
	}

	private static Connection connection(AtomicBoolean autoCommit, AtomicInteger commits, AtomicInteger rollbacks)
	{
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, (proxy, method, args) -> switch (method.getName())
		{
			case "getAutoCommit" -> autoCommit.get();
			case "setAutoCommit" ->
			{
				autoCommit.set((Boolean) args[0]);
				yield null;
			}
			case "commit" ->
			{
				commits.incrementAndGet();
				yield null;
			}
			case "rollback" ->
			{
				rollbacks.incrementAndGet();
				yield null;
			}
			case "close" -> null;
			default -> method.getReturnType().isPrimitive() ? primitiveDefault(method.getReturnType()) : null;
		});
	}

	private static Object primitiveDefault(Class<?> type)
	{
		if (type == boolean.class)
			return false;
		if (type == byte.class)
			return (byte) 0;
		if (type == short.class)
			return (short) 0;
		if (type == int.class)
			return 0;
		if (type == long.class)
			return 0L;
		if (type == float.class)
			return 0F;
		if (type == double.class)
			return 0D;
		if (type == char.class)
			return '\0';
		return null;
	}
}
