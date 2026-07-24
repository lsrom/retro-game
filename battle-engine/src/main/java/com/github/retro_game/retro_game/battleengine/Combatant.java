package com.github.retro_game.retro_game.battleengine;

import java.util.EnumMap;

public record Combatant(
        long userId,
        CombatantCoordinates coordinates,
        int weaponsTechnology,
        int shieldingTechnology,
        int armorTechnology,
        EnumMap<UnitKind, Long> unitGroups
) {
}
