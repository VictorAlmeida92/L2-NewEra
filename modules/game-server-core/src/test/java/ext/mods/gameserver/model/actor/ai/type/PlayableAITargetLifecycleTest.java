package ext.mods.gameserver.model.actor.ai.type;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.actor.player.PlayerDeath;
import ext.mods.gameserver.model.actor.ai.Intention;
import ext.mods.gameserver.enums.IntentionType;

class PlayableAITargetLifecycleTest
{
	@Test
	void deadTargetIsClassifiedAsStale()
	{
		final Player target = barePlayer();

		assertFalse(PlayableAI.isStaleCombatTarget(target));
		target.setIsDead(true);
		assertTrue(PlayableAI.isStaleCombatTarget(target));
	}

	@Test
	void missingTargetIsClassifiedAsStale()
	{
		assertTrue(PlayableAI.isStaleCombatTarget(null));
	}

	@Test
	void movementIntentionWithNoCombatTargetIsNotDiscarded()
	{
		final Intention movement = new Intention();
		movement.updateAsMoveTo(new ext.mods.gameserver.model.location.Location(10, 20, 30), null);

		assertEquals(IntentionType.MOVE_TO, movement.getType());
		assertFalse(PlayableAI.isStaleCombatIntention(movement));
	}

	private static Player barePlayer()
	{
		try
		{
			final Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			final sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
			final Player player = (Player) unsafe.allocateInstance(Player.class);
			final Field deathField = Player.class.getDeclaredField("_death");
			unsafe.putObject(player, unsafe.objectFieldOffset(deathField), new PlayerDeath(player));
			return player;
		}
		catch (Exception e)
		{
			throw new AssertionError("Could not create test player", e);
		}
	}
}
