package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.dto.UnitKindDto;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.model.behavior.ItemBehaviorRegistry;
import com.github.retro_game.retro_game.service.RapidFireTableService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Service
class RapidFireTableServiceImpl implements RapidFireTableService {
  private final Map<UnitKindDto, Map<UnitKindDto, Integer>> rapidFireTable;

  public RapidFireTableServiceImpl() {
    rapidFireTable = Collections.unmodifiableMap(generateRapidFireTable());
  }

  private Map<UnitKindDto, Map<UnitKindDto, Integer>> generateRapidFireTable() {
    Map<UnitKindDto, Map<UnitKindDto, Integer>> rapidFireTable = new EnumMap<>(UnitKindDto.class);

    // Rapid fire is code-only behavior, with no catalog representation; it is
    // read from the kind-keyed behavior registry. The table covers every
    // built-in unit kind, as it did when it was built from UnitItem.getAll().
    for (UnitKind rowKind : UnitKind.values()) {
      Map<UnitKind, Integer> rapidFireAgainst = ItemBehaviorRegistry.unitBehavior(rowKind.name()).getRapidFireAgainst();

      Map<UnitKindDto, Integer> rf = new EnumMap<>(UnitKindDto.class);
      for (UnitKind colKind : UnitKind.values()) {
        Integer n = rapidFireAgainst.get(colKind);
        if (n != null) {
          rf.put(Converter.convert(colKind), n);
          continue;
        }

        final Map<UnitKind, Integer> rapidFireFrom =
            ItemBehaviorRegistry.unitBehavior(colKind.name()).getRapidFireAgainst();
        n = rapidFireFrom.get(rowKind);
        if (n != null) {
          rf.put(Converter.convert(colKind), -n);
          continue;
        }

        rf.put(Converter.convert(colKind), 0);
      }

      rapidFireTable.put(Converter.convert(rowKind), rf);
    }

    return rapidFireTable;
  }

  @Override
  public Map<UnitKindDto, Map<UnitKindDto, Integer>> getRapidFireTable(long bodyId) {
    return rapidFireTable;
  }
}
