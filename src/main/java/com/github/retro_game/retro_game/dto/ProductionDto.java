package com.github.retro_game.retro_game.dto;

public record ProductionDto(
        double efficiency,
        long metalBaseProduction,
        long crystalBaseProduction,
        long deuteriumBaseProduction,
        long metalMineProduction,
        int metalMineCurrentEnergyUsage,
        int metalMineMaxEnergyUsage,
        long crystalMineProduction,
        int crystalMineCurrentEnergyUsage,
        int crystalMineMaxEnergyUsage,
        long deuteriumSynthesizerProduction,
        int deuteriumSynthesizerCurrentEnergyUsage,
        int deuteriumSynthesizerMaxEnergyUsage,
        int solarPlantEnergyProduction,
        long fusionReactorDeuteriumUsage,
        int fusionReactorEnergyProduction,
        int singleSolarSatelliteEnergyProduction,
        int solarSatellitesEnergyProduction,
        long metalProduction,
        long crystalProduction,
        long deuteriumProduction,
        int totalEnergy,
        int usedEnergy,
        int availableEnergy,
        long plasmaMetalBonus,
        long plasmaCrystalBonus,
        long plasmaDeuteriumBonus
) {
}
