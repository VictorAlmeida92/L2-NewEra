package ext.mods.gameserver.data.adapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import ext.mods.commons.logging.CLogger;
import ext.mods.commons.pool.ConnectionPool;
import ext.mods.gameserver.data.repository.CastleStore;

/** JDBC adapter for the castle manager persistence port. */
public final class JdbcCastleStore implements CastleStore
{
	private static final CLogger LOGGER = new CLogger(JdbcCastleStore.class.getName());

	private static final String LOAD_CASTLES = "SELECT * FROM castle ORDER BY id";
	private static final String LOAD_OWNER = "SELECT clan_id FROM clan_data WHERE hasCastle=?";
	private static final String LOAD_TRAPS = "SELECT * FROM castle_trapupgrade WHERE castleId=?";
	private static final String LOAD_DOORS = "SELECT * FROM castle_doorupgrade WHERE castleId=?";
	private static final String RESET_CERTIFICATES = "UPDATE castle SET certificates=300";

	@Override
	public List<CastleRecord> loadCastles()
	{
		final List<CastleRecord> castles = new ArrayList<>();
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(LOAD_CASTLES); ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				castles.add(new CastleRecord(rs.getInt("id"), rs.getLong("siegeDate"), rs.getBoolean("regTimeOver"), rs.getInt("currentTaxPercent"),
					rs.getInt("nextTaxPercent"), rs.getLong("treasury"), rs.getLong("taxRevenue"), rs.getLong("seedIncome"), rs.getInt("certificates")));
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load castles.", e);
		}
		return castles;
	}

	@Override
	public List<Integer> loadOwnerClanIds(int castleId)
	{
		final List<Integer> ownerIds = new ArrayList<>();
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(LOAD_OWNER))
		{
			ps.setInt(1, castleId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					ownerIds.add(rs.getInt("clan_id"));
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load owner for castle {}.", e, castleId);
		}
		return ownerIds;
	}

	@Override
	public List<UpgradeRecord> loadTrapUpgrades(int castleId)
	{
		return loadUpgrades(LOAD_TRAPS, "towerIndex", castleId);
	}

	@Override
	public List<UpgradeRecord> loadDoorUpgrades(int castleId)
	{
		return loadUpgrades(LOAD_DOORS, "doorId", castleId);
	}

	private List<UpgradeRecord> loadUpgrades(String sql, String idColumn, int castleId)
	{
		final List<UpgradeRecord> upgrades = new ArrayList<>();
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, castleId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					upgrades.add(new UpgradeRecord(rs.getInt(idColumn), rs.getInt("level")));
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load castle upgrades for {}.", e, castleId);
		}
		return upgrades;
	}

	@Override
	public void resetCertificates()
	{
		try (Connection con = ConnectionPool.getConnection(); PreparedStatement ps = con.prepareStatement(RESET_CERTIFICATES))
		{
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to reset castle certificates.", e);
		}
	}
}
