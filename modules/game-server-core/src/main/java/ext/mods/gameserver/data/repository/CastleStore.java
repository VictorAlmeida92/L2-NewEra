package ext.mods.gameserver.data.repository;

import java.util.List;

/** Persistence port for the castle manager's database-backed state. */
public interface CastleStore
{
	List<CastleRecord> loadCastles();

	List<Integer> loadOwnerClanIds(int castleId);

	List<UpgradeRecord> loadTrapUpgrades(int castleId);

	List<UpgradeRecord> loadDoorUpgrades(int castleId);

	void resetCertificates();

	record CastleRecord(int id, long siegeDate, boolean registrationOver, int currentTaxPercent, int nextTaxPercent,
		long treasury, long taxRevenue, long seedIncome, int certificates)
	{
	}

	record UpgradeRecord(int id, int level)
	{
	}
}
