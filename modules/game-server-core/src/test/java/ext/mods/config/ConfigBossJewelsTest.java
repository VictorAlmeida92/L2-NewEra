package ext.mods.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import ext.mods.Config;

import org.junit.jupiter.api.Test;

class ConfigBossJewelsTest
{
	@Test
	void usesTheCaseSensitiveRuntimeConfigFileName()
	{
		assertEquals("bossJewelUpgrades.properties", Path.of(Config.BOSS_JEWEL_UPGRADES_FILE).getFileName().toString());
	}
}
