(function () {
  const UNIT_COLOR_PRESETS = [
    ['SMALL_CARGO', 'S.Cargo', '#d7aa63'],
    ['LARGE_CARGO', 'L.Cargo', '#d7aa63'],
    ['LITTLE_FIGHTER', 'L.Fighter', '#93d66b'],
    ['HEAVY_FIGHTER', 'H.Fighter', '#93d66b'],
    ['CRUISER', 'Cruiser', '#59b4d8'],
    ['BATTLESHIP', 'B.Ship', '#e9a93b'],
    ['COLONY_SHIP', 'Col. Ship', '#d7aa63'],
    ['RECYCLER', 'Recycler', '#8ed0d8'],
    ['ESPIONAGE_PROBE', 'Probe', '#b9c7d8'],
    ['BOMBER', 'Bomber', '#d384e6'],
    ['SOLAR_SATELLITE', 'Sol. Sat', '#00a99d'],
    ['DESTROYER', 'Dest.', '#f06c64'],
    ['DEATH_STAR', 'Rip', '#d9dce7'],
    ['BATTLE_CRUISER', 'B.Cruiser', '#7cc8ff'],
    ['ROCKET_LAUNCHER', 'Miss.', '#95d979'],
    ['LIGHT_LASER', 'S.Laser', '#d7aa63'],
    ['HEAVY_LASER', 'H.Laser', '#93d66b'],
    ['GAUSS_CANNON', 'Gauss', '#b020b8'],
    ['ION_CANNON', 'Ion.C', '#a8b5c8'],
    ['PLASMA_TURRET', 'Plasma', '#baf0a4'],
    ['SMALL_SHIELD_DOME', 'S.Dome', '#d7aa63'],
    ['LARGE_SHIELD_DOME', 'LS.Dome', '#d7aa63'],
  ];
  const unitColorPresetsByKind = new Map(
    UNIT_COLOR_PRESETS.map(([kind, abbreviation, color]) => [kind, { abbreviation, color }]),
  );
  const numberFormatter = new Intl.NumberFormat();
  const DEFAULT_TEMPLATE_URL = '/sim-prettified-export-template.html';
  const EXPORT_TEMPLATES = [
    ['Table', DEFAULT_TEMPLATE_URL],
    ['Classic', '/sim-prettified-export-classic-template.html'],
    ['Roster', '/sim-prettified-export-roster-template.html'],
  ];
  const reportTemplatePromises = new Map();

  function formatNumber(value) {
    return numberFormatter.format(Math.round(Number(value) || 0));
  }

  function formatPercent(value) {
    return `${(Number(value) || 0).toFixed(2)}%`;
  }

  function resourceTotal(resources) {
    if (!resources) {
      return 0;
    }

    return (resources.metal ?? 0) + (resources.crystal ?? 0) + (resources.deuterium ?? 0);
  }

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (char) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    }[char]));
  }

  function renderTemplate(template, values) {
    return Object.entries(values).reduce(
      (html, [key, value]) => html.split(`{{${key}}}`).join(value),
      template,
    );
  }

  async function loadReportTemplate(templateUrl) {
    if (!reportTemplatePromises.has(templateUrl)) {
      reportTemplatePromises.set(
        templateUrl,
        fetch(templateUrl)
        .then(async (response) => {
          const text = await response.text();
          if (!response.ok) {
            throw new Error(text || response.statusText);
          }
          return text;
        }),
      );
    }
    return reportTemplatePromises.get(templateUrl);
  }

  function prettifiedNumber(value) {
    return formatNumber(value).replace(/,/g, '.');
  }

  function unitColorPreset(unit) {
    return unitColorPresetsByKind.get(unit.kind) ?? {
      abbreviation: unit.name,
      color: '#aeb7c5',
    };
  }

  function reportUnitsForCombatant(units, combatant, finalStats) {
    return units.filter((unit) =>
      (combatant.unitGroups.get(unit.kind) ?? 0) > 0 || (finalStats?.[unit.kind]?.numRemainingUnits ?? 0) > 0,
    );
  }

  function renderTableCell(tagName, unit, value, className = '') {
    const preset = unitColorPreset(unit);
    return `<${tagName} class="${className}" style="color:${escapeHtml(preset.color)}">${escapeHtml(value)}</${tagName}>`;
  }

  function coloredSpan(text, color) {
    return `<span style="color:${escapeHtml(color)}">${escapeHtml(text)}</span>`;
  }

  function renderCountCells(units, countForUnit, className = '') {
    return units
      .map((unit) => renderTableCell('td', unit, prettifiedNumber(countForUnit(unit)), className))
      .join('');
  }

  function renderLossCells(units, combatant, finalStats) {
    return units
      .map((unit) => {
        const initialCount = combatant.unitGroups.get(unit.kind) ?? 0;
        const finalCount = finalStats?.[unit.kind]?.numRemainingUnits ?? 0;
        return renderTableCell('td', unit, `-${prettifiedNumber(initialCount - finalCount)}`, 'loss');
      })
      .join('');
  }

  function renderPrettifiedCombatant(units, combatant, finalStats) {
    const reportUnits = reportUnitsForCombatant(units, combatant, finalStats);
    const hasSurvivors = reportUnits.some((unit) => (finalStats?.[unit.kind]?.numRemainingUnits ?? 0) > 0);
    const headerCells = reportUnits
      .map((unit) => renderTableCell('th', unit, unitColorPreset(unit).abbreviation))
      .join('');
    const initialCells = renderCountCells(reportUnits, (unit) => combatant.unitGroups.get(unit.kind) ?? 0);
    const lossCells = renderLossCells(reportUnits, combatant, finalStats);
    const finalCells = renderCountCells(reportUnits, (unit) => finalStats?.[unit.kind]?.numRemainingUnits ?? 0);

    if (reportUnits.length === 0) {
      return `
        <section class="combatant">
          <div><span class="role">${escapeHtml(combatant.title)}</span> (${escapeHtml(combatant.coordinates)})</div>
          <div class="empty">No units</div>
        </section>
      `;
    }

    return `
      <section class="combatant">
        <div><span class="role">${escapeHtml(combatant.title)}</span> (${escapeHtml(combatant.coordinates)})</div>
        <table>
          <thead>
            <tr>${headerCells}</tr>
          </thead>
          <tbody>
            <tr>${initialCells}</tr>
            ${hasSurvivors ? `<tr>${lossCells}</tr><tr>${finalCells}</tr>` : `<tr><td class="destroyed" colspan="${reportUnits.length}">Destroyed</td></tr>`}
          </tbody>
        </table>
      </section>
    `;
  }

  function renderClassicUnitLines(reportUnits, countForUnit) {
    if (reportUnits.length === 0) {
      return [
        '<div class="unit-line">Type: -</div>',
        '<div class="unit-line">Number: -</div>',
      ].join('\n');
    }

    const labels = reportUnits
      .map((unit) => {
        const preset = unitColorPreset(unit);
        return coloredSpan(preset.abbreviation, preset.color);
      })
      .join(' ');
    const counts = reportUnits
      .map((unit) => {
        const preset = unitColorPreset(unit);
        return coloredSpan(prettifiedNumber(countForUnit(unit)), preset.color);
      })
      .join(' ');

    return [
      `<div class="unit-line">Type: ${labels}</div>`,
      `<div class="unit-line">Number: ${counts}</div>`,
    ].join('\n');
  }

  function renderClassicInitialCombatant(units, combatant) {
    const reportUnits = units.filter((unit) => (combatant.unitGroups.get(unit.kind) ?? 0) > 0);
    return `
      <section class="combatant">
        <div><span class="role">${escapeHtml(combatant.title)}</span> (${escapeHtml(combatant.coordinates)})</div>
        <div>Weapons: ${combatant.weaponsTechnology * 10}% Shields: ${combatant.shieldingTechnology * 10}% Hull Plating: ${combatant.armorTechnology * 10}%</div>
        ${renderClassicUnitLines(reportUnits, (unit) => combatant.unitGroups.get(unit.kind) ?? 0)}
      </section>
    `;
  }

  function renderClassicFinalCombatant(units, combatant, finalStats) {
    const reportUnits = units.filter((unit) => (finalStats?.[unit.kind]?.numRemainingUnits ?? 0) > 0);
    return `
      <section class="combatant">
        <div><span class="role">${escapeHtml(combatant.title)}</span> (${escapeHtml(combatant.coordinates)})</div>
        ${renderClassicUnitLines(reportUnits, (unit) => finalStats?.[unit.kind]?.numRemainingUnits ?? 0)}
      </section>
    `;
  }

  function sideLabelClass(side) {
    return side === 'attacker' ? 'attacker-label' : 'defender-label';
  }

  function sideLineClass(side) {
    return side === 'attacker' ? 'attacker-line' : 'defender-line';
  }

  function renderRosterCombatant(units, combatant, finalStats, side, final) {
    const reportUnits = final
      ? reportUnitsForCombatant(units, combatant, finalStats)
      : units.filter((unit) => (combatant.unitGroups.get(unit.kind) ?? 0) > 0);
    const separatorClass = side === 'attacker' ? 'attacker-separator' : 'defender-separator';
    const unitLines = reportUnits.length === 0
      ? '<div class="empty">No units</div>'
      : reportUnits.map((unit) => {
        const initialCount = combatant.unitGroups.get(unit.kind) ?? 0;
        const finalCount = finalStats?.[unit.kind]?.numRemainingUnits ?? 0;
        const count = final ? finalCount : initialCount;
        const lost = initialCount - finalCount;
        const loss = final ? ` (-${prettifiedNumber(lost)})` : '';
        return `<div class="${sideLineClass(side)}">${escapeHtml(unit.name)} ${prettifiedNumber(count)}${loss}</div>`;
      }).join('\n');

    return `
      <section class="section">
        <div class="combatant-title"><span class="${sideLabelClass(side)}">${escapeHtml(combatant.title)}</span> ${escapeHtml(combatant.coordinates)}</div>
        ${unitLines}
        <div class="separator ${separatorClass}">------------------------------------------------------------</div>
      </section>
    `;
  }

  function lastRoundStats(output, side) {
    const outcomes = side === 'attacker' ? output.outcome?.attackersOutcomes : output.outcome?.defendersOutcomes;
    const stats = outcomes?.[0]?.unitGroupsStats;
    return stats?.[stats.length - 1] ?? null;
  }

  function unitColorCsv() {
    const rows = ['unit,abbreviation,color'];
    for (const [kind, abbreviation, color] of UNIT_COLOR_PRESETS) {
      rows.push(`${kind},${abbreviation},${color}`);
    }
    return rows.join('\n');
  }

  function prettifiedOutcomeHtml(output) {
    if (output.result === 'AttackerWins') {
      return `Attacker captures <span class="number">${prettifiedNumber(output.possiblePlunder?.metal)}</span> Metal, <span class="number">${prettifiedNumber(output.possiblePlunder?.crystal)}</span> Crystal and <span class="number">${prettifiedNumber(output.possiblePlunder?.deuterium)}</span> Deuterium.`;
    }
    if (output.result === 'DefenderWins') {
      return 'The defender has won the battle.';
    }
    return 'The battle ended in a draw.';
  }

  function rosterOutcomeHtml(output) {
    if (output.result === 'AttackerWins') {
      return `The attacker captured <span class="number">${prettifiedNumber(output.possiblePlunder?.metal)}</span> Metal, <span class="number">${prettifiedNumber(output.possiblePlunder?.crystal)}</span> Crystal and <span class="number">${prettifiedNumber(output.possiblePlunder?.deuterium)}</span> Deuterium.`;
    }
    if (output.result === 'DefenderWins') {
      return 'The defender has won the battle.';
    }
    return 'The battle ends in a draw.';
  }

  function renderRosterSummary(output) {
    return `
      <section class="summary">
        <div>${rosterOutcomeHtml(output)}</div>
        <div>The attacker lost a total of <span class="number">${prettifiedNumber(resourceTotal(output.lossesAttacker))}</span> units.</div>
        <div>The defender lost a total of <span class="number">${prettifiedNumber(resourceTotal(output.lossesDefender))}</span> units.</div>
        <br>
        <div>At these space coordinates now float <span class="number">${prettifiedNumber(output.debris?.metal)}</span> metal and <span class="number">${prettifiedNumber(output.debris?.crystal)}</span> crystal.</div>
        <div>The chance for a moon to be created from the debris was <span class="number">${formatPercent(output.moonchance)}</span>.</div>
        <div class="summary-title">Summary attackers(s)</div>
        <div>Metal: <span class="profit">${prettifiedNumber((output.possiblePlunder?.metal ?? 0) - (output.lossesAttacker?.metal ?? 0))}</span></div>
        <div>Crystal: <span class="profit">${prettifiedNumber((output.possiblePlunder?.crystal ?? 0) - (output.lossesAttacker?.crystal ?? 0))}</span></div>
        <div>Deuterium: <span class="profit">${prettifiedNumber((output.possiblePlunder?.deuterium ?? 0) - (output.lossesAttacker?.deuterium ?? 0))}</span></div>
        <div>The attacker(s) made a profit of <span class="profit">${prettifiedNumber(resourceTotal(output.possiblePlunder) - resourceTotal(output.lossesAttacker))}</span> units.</div>
        <div class="summary-title">Summary defenders(s)</div>
        <div>Metal: <span class="loss">-${prettifiedNumber(output.lossesDefender?.metal)}</span></div>
        <div>Crystal: <span class="loss">-${prettifiedNumber(output.lossesDefender?.crystal)}</span></div>
        <div>Deuterium: <span class="loss">-${prettifiedNumber(output.lossesDefender?.deuterium)}</span></div>
        <div>The defender(s) lost a total of <span class="number">${prettifiedNumber(resourceTotal(output.lossesDefender))}</span> units.</div>
      </section>
    `;
  }

  async function buildPrettifiedReportHtml(output, input, units, templateUrl) {
    const attackerFinalStats = lastRoundStats(output, 'attacker');
    const defenderFinalStats = lastRoundStats(output, 'defender');
    const template = await loadReportTemplate(templateUrl);

    return renderTemplate(template, {
      unitColorCsv: escapeHtml(unitColorCsv()),
      attackerFleet: renderPrettifiedCombatant(units, input.attacker, attackerFinalStats),
      attackerInitialFleet: renderClassicInitialCombatant(units, input.attacker),
      attackerFinalFleet: renderClassicFinalCombatant(units, input.attacker, attackerFinalStats),
      attackerRosterInitial: renderRosterCombatant(units, input.attacker, attackerFinalStats, 'attacker', false),
      attackerRosterFinal: renderRosterCombatant(units, input.attacker, attackerFinalStats, 'attacker', true),
      attackerLosses: prettifiedNumber(resourceTotal(output.lossesAttacker)),
      defenderFleet: renderPrettifiedCombatant(units, input.defender, defenderFinalStats),
      defenderInitialFleet: renderClassicInitialCombatant(units, input.defender),
      defenderFinalFleet: renderClassicFinalCombatant(units, input.defender, defenderFinalStats),
      defenderRosterInitial: renderRosterCombatant(units, input.defender, defenderFinalStats, 'defender', false),
      defenderRosterFinal: renderRosterCombatant(units, input.defender, defenderFinalStats, 'defender', true),
      outcome: prettifiedOutcomeHtml(output),
      rosterSummary: renderRosterSummary(output),
      defenderLosses: prettifiedNumber(resourceTotal(output.lossesDefender)),
      debrisMetal: prettifiedNumber(output.debris?.metal),
      debrisCrystal: prettifiedNumber(output.debris?.crystal),
      moonchance: formatPercent(output.moonchance),
    });
  }

  window.exportPrettifiedReport = async function exportPrettifiedReport(output, input, units, templateUrl = DEFAULT_TEMPLATE_URL) {
    const html = await buildPrettifiedReportHtml(output, input, units, templateUrl);
    const blob = new Blob([html], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `combat-report-${output.outcome?.seed ?? 'sim'}.html`;
    document.body.append(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  window.prettifiedReportTemplates = EXPORT_TEMPLATES.map(([label, templateUrl]) => ({ label, templateUrl }));
  for (const [, templateUrl] of EXPORT_TEMPLATES) {
    loadReportTemplate(templateUrl).catch(() => {});
  }
}());
