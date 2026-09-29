package ext.mods.gameserver.data;

import ext.mods.gameserver.data.adapter.JdbcBookmarkStore;
import ext.mods.gameserver.data.adapter.JdbcBufferSchemeStore;
import ext.mods.gameserver.data.adapter.JdbcBuyListStore;
import ext.mods.gameserver.data.adapter.JdbcCastleStore;
import ext.mods.gameserver.data.adapter.JdbcCharacterSelectionStore;
import ext.mods.gameserver.data.adapter.JdbcCharacterStore;
import ext.mods.gameserver.data.adapter.JdbcClanHallAuctionStore;
import ext.mods.gameserver.data.adapter.JdbcClanStore;
import ext.mods.gameserver.data.adapter.JdbcCustomEventStateStore;
import ext.mods.gameserver.data.adapter.JdbcGameServerRegistrationStore;
import ext.mods.gameserver.data.adapter.JdbcItemStore;
import ext.mods.gameserver.data.adapter.JdbcOfflineTraderStore;
import ext.mods.gameserver.data.adapter.JdbcOlympiadStore;
import ext.mods.gameserver.data.adapter.JdbcPlayerAuxiliaryStore;
import ext.mods.gameserver.data.adapter.JdbcPlayerInfoStore;
import ext.mods.gameserver.data.adapter.JdbcPremiumStore;
import ext.mods.gameserver.data.adapter.JdbcQuestStore;
import ext.mods.gameserver.data.adapter.JdbcRecommendationStore;
import ext.mods.gameserver.data.adapter.JdbcServerMemoStore;
import ext.mods.gameserver.data.adapter.JdbcSkillStore;
import ext.mods.gameserver.data.adapter.JdbcSubclassStore;
import ext.mods.gameserver.data.repository.BookmarkStore;
import ext.mods.gameserver.data.repository.BufferSchemeStore;
import ext.mods.gameserver.data.repository.BuyListStore;
import ext.mods.gameserver.data.repository.CastleStore;
import ext.mods.gameserver.data.repository.CharacterSelectionStore;
import ext.mods.gameserver.data.repository.CharacterStore;
import ext.mods.gameserver.data.repository.ClanHallAuctionStore;
import ext.mods.gameserver.data.repository.ClanStore;
import ext.mods.gameserver.data.repository.CustomEventStateStore;
import ext.mods.gameserver.data.repository.GameServerRegistrationStore;
import ext.mods.gameserver.data.repository.ItemStore;
import ext.mods.gameserver.data.repository.OfflineTraderStore;
import ext.mods.gameserver.data.repository.OlympiadStore;
import ext.mods.gameserver.data.repository.PlayerAuxiliaryStore;
import ext.mods.gameserver.data.repository.PlayerInfoStore;
import ext.mods.gameserver.data.repository.PremiumStore;
import ext.mods.gameserver.data.repository.QuestStore;
import ext.mods.gameserver.data.repository.RecommendationStore;
import ext.mods.gameserver.data.repository.ServerMemoStore;
import ext.mods.gameserver.data.repository.SkillStore;
import ext.mods.gameserver.data.repository.SubclassStore;

/**
 * Composition root for the GameServer persistence adapters.
 *
 * <p>Application services and game models depend on repository ports. Only
 * this registry selects the current JDBC implementations, making a future
 * adapter/database change local to one boundary.</p>
 */
public final class PersistenceRegistry
{
	private static final BookmarkStore BOOKMARKS = new JdbcBookmarkStore();
	private static final BufferSchemeStore BUFFER_SCHEMES = new JdbcBufferSchemeStore();
	private static final BuyListStore BUY_LISTS = new JdbcBuyListStore();
	private static final CastleStore CASTLES = new JdbcCastleStore();
	private static final CharacterSelectionStore CHARACTER_SELECTION = new JdbcCharacterSelectionStore();
	private static final CharacterStore CHARACTERS = new JdbcCharacterStore();
	private static final ClanHallAuctionStore CLAN_HALL_AUCTIONS = new JdbcClanHallAuctionStore();
	private static final ClanStore CLANS = new JdbcClanStore();
	private static final CustomEventStateStore CUSTOM_EVENTS = new JdbcCustomEventStateStore();
	private static final GameServerRegistrationStore GAME_SERVER_REGISTRATION = new JdbcGameServerRegistrationStore();
	private static final ItemStore ITEMS = new JdbcItemStore();
	private static final OfflineTraderStore OFFLINE_TRADERS = new JdbcOfflineTraderStore();
	private static final OlympiadStore OLYMPIAD = new JdbcOlympiadStore();
	private static final PlayerAuxiliaryStore PLAYER_AUXILIARY = new JdbcPlayerAuxiliaryStore();
	private static final PlayerInfoStore PLAYER_INFO = new JdbcPlayerInfoStore();
	private static final PremiumStore PREMIUM = new JdbcPremiumStore();
	private static final QuestStore QUESTS = new JdbcQuestStore();
	private static final RecommendationStore RECOMMENDATIONS = new JdbcRecommendationStore();
	private static final ServerMemoStore SERVER_MEMOS = new JdbcServerMemoStore();
	private static final SkillStore SKILLS = new JdbcSkillStore();
	private static final SubclassStore SUBCLASSES = new JdbcSubclassStore();

	private PersistenceRegistry()
	{
	}

	public static BookmarkStore bookmarks() { return BOOKMARKS; }
	public static BufferSchemeStore bufferSchemes() { return BUFFER_SCHEMES; }
	public static BuyListStore buyLists() { return BUY_LISTS; }
	public static CastleStore castles() { return CASTLES; }
	public static CharacterSelectionStore characterSelection() { return CHARACTER_SELECTION; }
	public static CharacterStore characters() { return CHARACTERS; }
	public static ClanHallAuctionStore clanHallAuctions() { return CLAN_HALL_AUCTIONS; }
	public static ClanStore clans() { return CLANS; }
	public static CustomEventStateStore customEvents() { return CUSTOM_EVENTS; }
	public static GameServerRegistrationStore gameServerRegistration() { return GAME_SERVER_REGISTRATION; }
	public static ItemStore items() { return ITEMS; }
	public static OfflineTraderStore offlineTraders() { return OFFLINE_TRADERS; }
	public static OlympiadStore olympiad() { return OLYMPIAD; }
	public static PlayerAuxiliaryStore playerAuxiliary() { return PLAYER_AUXILIARY; }
	public static PlayerInfoStore playerInfo() { return PLAYER_INFO; }
	public static PremiumStore premium() { return PREMIUM; }
	public static QuestStore quests() { return QUESTS; }
	public static RecommendationStore recommendations() { return RECOMMENDATIONS; }
	public static ServerMemoStore serverMemos() { return SERVER_MEMOS; }
	public static SkillStore skills() { return SKILLS; }
	public static SubclassStore subclasses() { return SUBCLASSES; }
}
