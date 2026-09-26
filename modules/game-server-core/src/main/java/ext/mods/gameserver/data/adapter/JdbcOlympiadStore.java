package ext.mods.gameserver.data.adapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ext.mods.commons.jdbc.DatabaseDialect;
import ext.mods.commons.logging.CLogger;
import ext.mods.commons.pool.ConnectionPool;
import ext.mods.gameserver.data.repository.OlympiadStore;

/** JDBC adapter for the Olympiad persistence port. */
public final class JdbcOlympiadStore implements OlympiadStore
{
	private static final CLogger LOGGER = new CLogger(JdbcOlympiadStore.class.getName());

	private static final String LOAD_STATUS = "SELECT current_cycle, period, olympiad_end, validation_end, next_weekly_change FROM olympiad_data WHERE id=0";
	private static final String LOAD_NOBLES = "SELECT olympiad_nobles.char_id, olympiad_nobles.class_id, characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn FROM olympiad_nobles, characters WHERE characters.obj_Id=olympiad_nobles.char_id";
	private static final String LOAD_RANKED_NOBLES = "SELECT char_id FROM olympiad_nobles_eom WHERE competitions_done>=? ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC";
	private static final String LOAD_CLASS_LEADERS_MONTHLY = "SELECT characters.char_name FROM olympiad_nobles_eom, characters WHERE characters.obj_Id=olympiad_nobles_eom.char_id AND olympiad_nobles_eom.class_id=? AND olympiad_nobles_eom.competitions_done>=? ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
	private static final String LOAD_CLASS_LEADERS_CURRENT = "SELECT characters.char_name FROM olympiad_nobles, characters WHERE characters.obj_Id=olympiad_nobles.char_id AND olympiad_nobles.class_id=? AND olympiad_nobles.competitions_done>=3 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC LIMIT 10";
	private static final String LOAD_LAST_POINTS = "SELECT olympiad_points FROM olympiad_nobles_eom WHERE char_id=?";
	private static final String ARCHIVE_NOBLES = "INSERT INTO olympiad_nobles_eom (char_id,class_id,olympiad_points,competitions_done,competitions_won,competitions_lost,competitions_drawn) SELECT char_id,class_id,olympiad_points,competitions_done,competitions_won,competitions_lost,competitions_drawn FROM olympiad_nobles";
	private static final String DELETE_NOBLES = "TRUNCATE olympiad_nobles";
	private static final String DELETE_ARCHIVED_NOBLES = "TRUNCATE olympiad_nobles_eom";

	private static String saveNoblesSql()
	{
		return DatabaseDialect.upsert("olympiad_nobles", "char_id,class_id,olympiad_points,competitions_done,competitions_won,competitions_lost,competitions_drawn", "?,?,?,?,?,?,?", "char_id", "class_id,olympiad_points,competitions_done,competitions_won,competitions_lost,competitions_drawn");
	}

	private static String saveStatusSql()
	{
		return DatabaseDialect.upsert("olympiad_data", "id,current_cycle,period,olympiad_end,validation_end,next_weekly_change", "0,?,?,?,?,?", "id", "current_cycle,period,olympiad_end,validation_end,next_weekly_change");
	}

	@Override
	public Optional<OlympiadStatus> loadStatus()
	{
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(LOAD_STATUS); ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
				return Optional.of(new OlympiadStatus(rs.getInt("current_cycle"), rs.getString("period"), rs.getLong("olympiad_end"), rs.getLong("validation_end"), rs.getLong("next_weekly_change")));
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load Olympiad status.", e);
		}
		return Optional.empty();
	}

	@Override
	public List<NobleRecord> loadNobles()
	{
		final List<NobleRecord> nobles = new ArrayList<>();
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(LOAD_NOBLES); ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
				nobles.add(new NobleRecord(rs.getInt("char_id"), rs.getInt("class_id"), rs.getString("char_name"), rs.getInt("olympiad_points"), rs.getInt("competitions_done"), rs.getInt("competitions_won"), rs.getInt("competitions_lost"), rs.getInt("competitions_drawn")));
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load Olympiad nobles.", e);
		}
		return nobles;
	}

	@Override
	public List<Integer> loadRankedNobleIds(int minimumMatches)
	{
		final List<Integer> ids = new ArrayList<>();
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(LOAD_RANKED_NOBLES))
		{
			ps.setInt(1, minimumMatches);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					ids.add(rs.getInt("char_id"));
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load Olympiad ranks.", e);
		}
		return ids;
	}

	@Override
	public List<String> loadClassLeaders(int classId, int minimumMatches, boolean monthly)
	{
		final List<String> names = new ArrayList<>();
		final String sql = monthly ? LOAD_CLASS_LEADERS_MONTHLY : LOAD_CLASS_LEADERS_CURRENT;
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, classId);
			if (monthly)
				ps.setInt(2, minimumMatches);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					names.add(rs.getString("char_name"));
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load Olympiad leaders.", e);
		}
		return names;
	}

	@Override
	public int loadLastNoblePoints(int objectId)
	{
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(LOAD_LAST_POINTS))
		{
			ps.setInt(1, objectId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
					return rs.getInt("olympiad_points");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load last Olympiad points.", e);
		}
		return 0;
	}

	@Override
	public void saveNobles(Collection<NobleRecord> nobles)
	{
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(saveNoblesSql()))
		{
			for (NobleRecord noble : nobles)
			{
				ps.setInt(1, noble.charId());
				ps.setInt(2, noble.classId());
				ps.setInt(3, noble.points());
				ps.setInt(4, noble.competitionsDone());
				ps.setInt(5, noble.competitionsWon());
				ps.setInt(6, noble.competitionsLost());
				ps.setInt(7, noble.competitionsDrawn());
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to save Olympiad nobles.", e);
		}
	}

	@Override
	public void saveStatus(OlympiadStatus status)
	{
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(saveStatusSql()))
		{
			ps.setInt(1, status.currentCycle());
			ps.setString(2, status.period());
			ps.setLong(3, status.olympiadEnd());
			ps.setLong(4, status.validationEnd());
			ps.setLong(5, status.nextWeeklyChange());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to save Olympiad status.", e);
		}
	}

	@Override
	public void archiveNobles()
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			con.setAutoCommit(false);
			try (PreparedStatement clear = con.prepareStatement(DatabaseDialect.adapt(DELETE_ARCHIVED_NOBLES)); PreparedStatement archive = con.prepareStatement(ARCHIVE_NOBLES))
			{
				clear.executeUpdate();
				archive.executeUpdate();
				con.commit();
			}
			catch (Exception e)
			{
				con.rollback();
				throw e;
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to archive monthly Olympiad nobles.", e);
		}
	}

	@Override
	public void deleteNobles()
	{
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(DatabaseDialect.adapt(DELETE_NOBLES)))
		{
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to delete Olympiad nobles.", e);
		}
	}
}
