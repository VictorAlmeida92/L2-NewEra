# Repository contracts

This document records the persistence contracts introduced during Phase 4. Game
logic must depend on these interfaces and services rather than constructing SQL
or JDBC statements directly.

## `CharacterStore`

`ext.mods.gameserver.data.repository.CharacterStore` owns the persistence
operations required by the character lifecycle:

- `insert(state)` creates the initial character row;
- `update(state)` persists the rollback-sensitive base character state;
- `restore(objectId)` and `findAccountCharacters(account, excludedId)` load
  character state for login;
- `updateOnlineStatus(objectId, online, lastAccess)` records session state;
- `updateNobless(objectId, noble)` persists the noblesse flag;
- `findClanId(objectId)` reads the clan relationship needed before deletion;
- `updateDeleteTime(objectId, deleteTime)` schedules or restores deletion;
- `deleteCharacter(objectId)` removes the character and all owned records.

The contract deliberately does not expose `Connection`, `PreparedStatement`,
SQL strings, or a database-specific type. `JdbcCharacterStore` is the current
adapter and is responsible for SQL dialect details and connection handling.

The same contract also owns the basic character-row lifecycle used by creation,
login restoration, autosave/logout, online status and noblesse updates.
`CharacterState` is a value-only snapshot: the model assembles or applies it,
while `JdbcCharacterStore` performs the JDBC mapping. This keeps the
rollback-sensitive character row behind one persistence boundary without
moving gameplay decisions into the adapter.

### Transaction boundary

`deleteCharacter` is atomic from the caller's perspective: all related rows are
deleted in one database transaction, with rollback on failure. Cache and event
notifications happen only after the database operation succeeds, in the
application service `CharacterLifecycleService`.

### Migration rule

New game code must call the repository/service contract. A database-specific
query found outside an adapter is a Phase 4 cleanup candidate and should be
registered before being migrated. The adapter remains replaceable by another
implementation (for example, a future MariaDB or PostgreSQL-specific adapter)
without changing the game flow.

## `PremiumStore`

`ext.mods.gameserver.data.repository.PremiumStore` centralizes the
`account_premium` table used by player login, the donation item flow and the
administrator premium commands. Its operations preserve the existing behavior:

- `upsert` creates or updates the service and expiration timestamp;
- `find` reads the account state without creating it;
- `expire` clears an existing row;
- `delete` removes the row completely.

The JDBC implementation is `JdbcPremiumStore`, exposed to game code through
`PremiumPersistenceService`. This prevents the same table and dialect-specific
upsert logic from being repeated across unrelated handlers.

## `RecommendationStore`

`ext.mods.gameserver.data.repository.RecommendationStore` owns the
`character_recommends` row and the two counter updates that must accompany it.
`addRecommendation` commits all three writes atomically, while
`loadGivenRecommendations` restores the giver's target list. The in-memory
rollback behavior remains in `PlayerRecom`; database transaction handling is
implemented by `JdbcRecommendationStore`.

## `SubclassStore`

`ext.mods.gameserver.data.repository.SubclassStore` owns subclass slots and
the class-indexed data removed during a subclass replacement. `wipe` deletes
the subclass, hennas, shortcuts, saved effects and skills in one transaction;
the subsequent creation of the replacement slot remains orchestrated by
`PlayerSubClass`, preserving the existing game flow.

## `SkillStore`

`ext.mods.gameserver.data.repository.SkillStore` owns character skill rows
and saved effect/cooldown state. `JdbcSkillStore.replaceSkillSaves` replaces
the saved state in one transaction, while `PlayerSkillsDb` continues to own
skill lookup, effect construction and runtime cooldown decisions.

## `ItemStore`

`ext.mods.gameserver.data.repository.ItemStore` owns item restoration, item
upserts, augmentation state and pet-item cleanup. `JdbcItemStore` performs one
joined read for item plus augmentation state, preserving the old behavior while
removing the per-item SQL call from `ItemInstance`.

The periodic `ItemInstanceTaskManager` still controls when dirty items are
flushed; it now delegates the batch to `ItemPersistenceService`. The model
continues to decide which item state is dirty, while the adapter owns SQL,
upsert syntax and JDBC connections.

## `PlayerAuxiliaryStore`

`ext.mods.gameserver.data.repository.PlayerAuxiliaryStore` groups the
character-owned auxiliary state that is restored and saved around the player
session: macros, shortcuts, recipes, hennas, mission progress and memos.
`JdbcPlayerAuxiliaryStore` is the only class in this group that knows the table
names, JDBC statements and dialect-specific upserts. The player models keep
their validation, parsing and packet behavior and call
`PlayerAuxiliaryPersistenceService` for persistence.

## Repository matrix

The following matrix is the current composition boundary. Game code consumes
the port in the first column; the JDBC class in the second column is selected
only by `PersistenceRegistry`.

| Port | Current adapter | Main responsibility |
| --- | --- | --- |
| `BookmarkStore` | `JdbcBookmarkStore` | GM/player bookmarks |
| `BufferSchemeStore` | `JdbcBufferSchemeStore` | Saved buffer schemes |
| `BuyListStore` | `JdbcBuyListStore` | Buy-list restock state |
| `CastleStore` | `JdbcCastleStore` | Castles, owners, doors and traps |
| `CharacterSelectionStore` | `JdbcCharacterSelectionStore` | Account character selection |
| `CharacterStore` | `JdbcCharacterStore` | Character lifecycle and base state |
| `ClanHallAuctionStore` | `JdbcClanHallAuctionStore` | Clan-hall bids and sellers |
| `ClanStore` | `JdbcClanStore` | Clans, wars and ranking |
| `CustomEventStateStore` | `JdbcCustomEventStateStore` | Custom event enablement |
| `GameServerRegistrationStore` | `JdbcGameServerRegistrationStore` | LoginServer gameserver registry |
| `ItemStore` | `JdbcItemStore` | Inventory, augmentation and pet items |
| `OfflineTraderStore` | `JdbcOfflineTraderStore` | Offline trade snapshots |
| `OlympiadStore` | `JdbcOlympiadStore` | Olympiad cycle, nobles and rankings |
| `PlayerAuxiliaryStore` | `JdbcPlayerAuxiliaryStore` | Macros, shortcuts, recipes, hennas, missions and memos |
| `PlayerInfoStore` | `JdbcPlayerInfoStore` | Player directory/admin information |
| `PremiumStore` | `JdbcPremiumStore` | Account premium service and expiry |
| `QuestStore` | `JdbcQuestStore` | Quest variables and completion state |
| `RecommendationStore` | `JdbcRecommendationStore` | Recommendations and counters |
| `ServerMemoStore` | `JdbcServerMemoStore` | Server-wide key/value state |
| `SkillStore` | `JdbcSkillStore` | Learned skills and saved effects |
| `SubclassStore` | `JdbcSubclassStore` | Subclass slots and replacement cleanup |

## Contract rules

1. A port describes domain data and operations, not tables or SQL syntax.
2. Records crossing a port are value objects. They must not contain a JDBC
   connection, statement, result set or mutable adapter state.
3. Transactional behavior is part of the operation contract. Methods such as
   character deletion, subclass wipe, recommendation update and skill-save
   replacement must either complete their documented unit of work or leave the
   database unchanged.
4. Loading methods must not mutate gameplay state. The application service or
   model applies the returned records and owns validation and packet behavior.
5. The adapter owns SQL dialect selection, parameter binding, connection
   lifecycle and translation of database failures.
6. `PersistenceRegistry` is the only GameServer composition root for the
   current JDBC implementations. New game code must not instantiate a
   `Jdbc*Store` directly.

## Transitional limitations

Some ports still declare `java.sql.SQLException` for compatibility with the
existing call sites. This is the remaining JDBC leak in the public contract;
it does not expose a connection or SQL statement, but it couples error
handling to JDBC. Removing that exception from ports requires a separate,
tested error-mapping change and is therefore deferred to the architectural
modernization phase rather than being hidden in this documentation PR.

The repository contracts are currently verified by build scans and
`RepositoryContractBoundaryTest`. The test allows the transitional
`SQLException` declaration but rejects concrete JDBC types and SQL statements
inside the port package.
