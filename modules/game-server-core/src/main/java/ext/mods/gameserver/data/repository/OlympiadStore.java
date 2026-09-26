package ext.mods.gameserver.data.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistence port for the database-backed Olympiad state. */
public interface OlympiadStore
{
	Optional<OlympiadStatus> loadStatus();

	List<NobleRecord> loadNobles();

	List<Integer> loadRankedNobleIds(int minimumMatches);

	List<String> loadClassLeaders(int classId, int minimumMatches, boolean monthly);

	int loadLastNoblePoints(int objectId);

	void saveNobles(Collection<NobleRecord> nobles);

	void saveStatus(OlympiadStatus status);

	void archiveNobles();

	void deleteNobles();

	record OlympiadStatus(int currentCycle, String period, long olympiadEnd, long validationEnd, long nextWeeklyChange)
	{
	}

	record NobleRecord(int charId, int classId, String charName, int points, int competitionsDone, int competitionsWon, int competitionsLost, int competitionsDrawn)
	{
	}
}
