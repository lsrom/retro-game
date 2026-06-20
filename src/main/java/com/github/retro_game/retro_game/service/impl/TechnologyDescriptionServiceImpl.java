package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.service.TechnologyDescriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service("technologyDescriptionService")
class TechnologyDescriptionServiceImpl implements TechnologyDescriptionService {
  private final MessageSource messageSource;
  private final boolean laserTechnologyBoostsSolarSatellites;
  private final double laserTechnologyBoost;

  TechnologyDescriptionServiceImpl(
      MessageSource messageSource,
      @Value("${retro-game.laser-technology-boost-solar-satellites}")
      double laserTechnologyBoostSolarSatellites
  ) {
    this.messageSource = messageSource;
    this.laserTechnologyBoostsSolarSatellites = laserTechnologyBoostSolarSatellites > 0;
    this.laserTechnologyBoost = laserTechnologyBoostSolarSatellites;
  }

  @Nullable
  @Override
  public String getExtraDescription(String kind) {
    if ("LASER_TECHNOLOGY".equals(kind) && laserTechnologyBoostsSolarSatellites) {
      return messageSource.getMessage(
          "items.LASER_TECHNOLOGY.description.extra",
          new Object[]{laserTechnologyBoost * 100},
          LocaleContextHolder.getLocale()
      );
    }
    return null;
  }
}
