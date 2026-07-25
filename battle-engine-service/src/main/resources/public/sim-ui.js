const form = document.getElementById('sim-form');
const statusEl = document.getElementById('status');
const submitButton = document.getElementById('submit-button');
const seedInput = document.getElementById('seed-input');
const summaryEl = document.getElementById('summary');
const reportEl = document.getElementById('report');
const attackerUnitsEl = document.getElementById('attacker-units');
const defenderUnitsEl = document.getElementById('defender-units');
const numberFormatter = new Intl.NumberFormat();
let unitsByKind = new Map();

function setStatus(message, isError = false) {
  statusEl.textContent = message;
  statusEl.classList.toggle('error', isError);
}

function unitInputName(side, index) {
  return `ship_${side}0_${index}_b`;
}

function renderUnitInputs(units) {
  unitsByKind = new Map(units.map((unit) => [unit.kind, unit]));
  attackerUnitsEl.replaceChildren(createUnitColumn('a', units, false), createUnitColumn('a', units, true));
  defenderUnitsEl.replaceChildren(createUnitColumn('d', units, false), createUnitColumn('d', units, true));
}

function createUnitColumn(side, units, defensive) {
  const column = document.createElement('div');
  column.className = 'unit-column';

  if (side === 'a' && defensive) {
    column.setAttribute('aria-hidden', 'true');
    return column;
  }

  column.replaceChildren(
    ...units
      .filter((unit) => unit.defensive === defensive)
      .map((unit) => createUnitInput(side, unit)),
  );
  return column;
}

function createUnitInput(side, unit) {
  const label = document.createElement('label');
  label.className = 'unit-row';
  label.textContent = unit.name;

  const input = document.createElement('input');
  input.name = unitInputName(side, unit.index);
  input.type = 'number';
  input.min = '0';
  input.step = '1';
  input.value = '0';
  input.dataset.kind = unit.kind;

  label.append(input);
  return label;
}

function formToParams() {
  const params = new URLSearchParams();
  const data = new FormData(form);

  for (const [name, rawValue] of data.entries()) {
    const value = String(rawValue).trim();
    if (value === '') {
      continue;
    }

    if (name.startsWith('ship_') && value === '0') {
      continue;
    }

    params.set(name, value);
  }

  return params;
}

function prefillFormFromQuery() {
  const params = new URLSearchParams(window.location.search);
  for (const [name, value] of params.entries()) {
    const field = form.elements.namedItem(name);
    if (field instanceof HTMLInputElement) {
      field.value = value;
    }
  }
}

function randomizeSeed() {
  seedInput.value = String(Math.floor(Math.random() * 2147483647));
}

function initializeSeed() {
  if (!new URLSearchParams(window.location.search).has('seed')) {
    randomizeSeed();
  }
}

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

function makeElement(tagName, text, className) {
  const element = document.createElement(tagName);
  if (className) {
    element.className = className;
  }
  if (text !== undefined && text !== null) {
    element.textContent = text;
  }
  return element;
}

function appendTextWithStrong(parent, parts) {
  for (const part of parts) {
    if (part.strong) {
      parent.append(makeElement('strong', part.text));
    } else {
      parent.append(document.createTextNode(part.text));
    }
  }
}

function getInputValue(name) {
  const field = form.elements.namedItem(name);
  return field instanceof HTMLInputElement ? field.value : '';
}

function getInputNumber(name) {
  return Number.parseInt(getInputValue(name), 10) || 0;
}

function collectCombatant(side, title, coordinatesField) {
  const unitGroups = new Map();
  for (const unit of unitsByKind.values()) {
    const count = getInputNumber(unitInputName(side, unit.index));
    if (count > 0) {
      unitGroups.set(unit.kind, count);
    }
  }

  return {
    title,
    coordinates: getInputValue(coordinatesField) || '1:1:1',
    weaponsTechnology: getInputNumber(`tech_${side}0_0`),
    shieldingTechnology: getInputNumber(`tech_${side}0_1`),
    armorTechnology: getInputNumber(`tech_${side}0_2`),
    unitGroups,
  };
}

function collectReportInput() {
  return {
    attacker: collectCombatant('a', 'Attacker', 'attacker_pos'),
    defender: collectCombatant('d', 'Defender', 'enemy_pos'),
  };
}

function createRow(label, values) {
  const row = document.createElement('tr');
  row.append(makeElement('td', label));
  for (const value of values) {
    row.append(makeElement('td', value));
  }
  return row;
}

function wrapTable(table) {
  const wrapper = makeElement('div', null, 'table-scroll');
  wrapper.append(table);
  return wrapper;
}

function renderInitialCombatant(combatant) {
  const table = makeElement('table', null, 'combatant-table');
  const header = document.createElement('tr');
  const heading = makeElement('th', `${combatant.title} [${combatant.coordinates}]`);
  heading.colSpan = Math.max(combatant.unitGroups.size + 1, 2);
  header.append(heading);

  const techRow = document.createElement('tr');
  const techCell = makeElement(
    'td',
    `${combatant.weaponsTechnology * 10}% weapons  ${combatant.shieldingTechnology * 10}% shields  ${combatant.armorTechnology * 10}% armor`,
  );
  techCell.colSpan = heading.colSpan;
  techRow.append(techCell);

  table.append(header, techRow);

  if (combatant.unitGroups.size === 0) {
    table.append(createRow('Kind', ['No units']));
    return wrapTable(table);
  }

  const units = [...combatant.unitGroups.keys()].map((kind) => unitsByKind.get(kind)).filter(Boolean);
  table.append(
    createRow('Kind', units.map((unit) => unit.name)),
    createRow('Total', units.map((unit) => formatNumber(combatant.unitGroups.get(unit.kind)))),
    createRow('Weapons', units.map((unit) => formatNumber(unit.weapons * (1 + combatant.weaponsTechnology / 10)))),
    createRow('Shields', units.map((unit) => formatNumber(unit.shield * (1 + combatant.shieldingTechnology / 10)))),
    createRow('Armor', units.map((unit) => formatNumber(unit.armor * (1 + combatant.armorTechnology / 10)))),
  );
  return wrapTable(table);
}

function renderInitialSide(combatant) {
  const side = makeElement('div', null, 'report-side');
  side.append(renderInitialCombatant(combatant));
  return side;
}

function orderedRoundEntries(stats) {
  if (!stats) {
    return [];
  }

  return [...unitsByKind.values()]
    .filter((unit) => (stats[unit.kind]?.numRemainingUnits ?? 0) > 0)
    .map((unit) => [unit, stats[unit.kind]]);
}

function renderRoundCombatant(title, stats) {
  const entries = orderedRoundEntries(stats);
  const table = makeElement('table', null, 'unit-stats');
  const header = document.createElement('tr');
  const heading = makeElement('th', title);
  heading.colSpan = Math.max(entries.length + 1, 2);
  header.append(heading);
  table.append(header);

  if (entries.length === 0) {
    table.append(createRow('Kind', ['No units']));
    return wrapTable(table);
  }

  table.append(
    createRow('Kind', entries.map(([unit]) => unit.name)),
    createRow('Remaining units', entries.map(([, stats]) => formatNumber(stats.numRemainingUnits))),
    createRow('Times fired', entries.map(([, stats]) => formatNumber(stats.timesFired))),
    createRow('Times was shot', entries.map(([, stats]) => formatNumber(stats.timesWasShot))),
    createRow(
      'Damage dealt',
      entries.map(([, stats]) => formatNumber((stats.shieldDamageDealt ?? 0) + (stats.hullDamageDealt ?? 0))),
    ),
    createRow(
      'Damage taken',
      entries.map(([, stats]) => formatNumber((stats.shieldDamageTaken ?? 0) + (stats.hullDamageTaken ?? 0))),
    ),
  );
  return wrapTable(table);
}

function renderRound(output, roundIndex) {
  const round = makeElement('div', null, 'round');
  round.append(makeElement('h3', `Round ${roundIndex + 1}:`));
  round.append(
    renderRoundCombatant('Attacker', output.outcome?.attackersOutcomes?.[0]?.unitGroupsStats?.[roundIndex]),
    renderRoundCombatant('Defender', output.outcome?.defendersOutcomes?.[0]?.unitGroupsStats?.[roundIndex]),
  );
  return round;
}

function renderOutcome(output) {
  const fragment = document.createDocumentFragment();
  const result = output.result;
  const outcome = document.createElement('p');

  if (result === 'AttackerWins') {
    appendTextWithStrong(outcome, [
      { text: 'The attackers won the battle and captured ' },
      { text: formatNumber(output.possiblePlunder?.metal), strong: true },
      { text: ' metal, ' },
      { text: formatNumber(output.possiblePlunder?.crystal), strong: true },
      { text: ' crystal and ' },
      { text: formatNumber(output.possiblePlunder?.deuterium), strong: true },
      { text: ' deuterium.' },
    ]);
  } else if (result === 'DefenderWins') {
    outcome.textContent = 'The defenders won the battle.';
  } else {
    outcome.textContent = 'The battle ended in a draw.';
  }

  const attackerLosses = document.createElement('p');
  appendTextWithStrong(attackerLosses, [
    { text: 'The attackers have lost a total of ' },
    { text: formatNumber(resourceTotal(output.lossesAttacker)), strong: true },
    { text: ' units.' },
  ]);

  const defenderLosses = document.createElement('p');
  appendTextWithStrong(defenderLosses, [
    { text: 'The defenders have lost a total of ' },
    { text: formatNumber(resourceTotal(output.lossesDefender)), strong: true },
    { text: ' units.' },
  ]);

  const debris = document.createElement('p');
  appendTextWithStrong(debris, [
    { text: 'At these space coordinates now float ' },
    { text: formatNumber(output.debris?.metal), strong: true },
    { text: ' metal and ' },
    { text: formatNumber(output.debris?.crystal), strong: true },
    { text: ' crystal.' },
  ]);

  const seed = document.createElement('p');
  appendTextWithStrong(seed, [
    { text: 'Seed: ' },
    { text: formatNumber(output.outcome?.seed), strong: true },
  ]);

  fragment.append(outcome, attackerLosses, defenderLosses, debris, seed);
  return fragment;
}

function renderReport(output, input) {
  const intro = makeElement('p', 'The following fleets met in battle:');
  const rounds = [];
  for (let roundIndex = 0; roundIndex < (output.outcome?.numRounds ?? 0); roundIndex += 1) {
    rounds.push(renderRound(output, roundIndex));
  }

  reportEl.replaceChildren(
    intro,
    renderInitialSide(input.attacker),
    renderInitialSide(input.defender),
    ...rounds,
    makeElement('hr'),
    renderOutcome(output),
  );
}

function renderSummary(output) {
  const rows = [
    ['Outcome', output, false],
    ['Debris', output.debris, true],
    ['Plunder', output.possiblePlunder, true],
    ['Attacker Losses', output.lossesAttacker, true],
    ['Defender Losses', output.lossesDefender, true],
  ];

  summaryEl.replaceChildren(...rows.map(([label, value, isResources]) => {
    const item = document.createElement('div');
    item.className = 'metric';

    const title = document.createElement('strong');
    title.textContent = label;

    const body = document.createElement('span');
    if (isResources) {
      body.replaceChildren(label === 'Debris' ? renderDebrisLines(output) : renderResourceLines(value));
    } else if (label === 'Outcome') {
      body.replaceChildren(renderOutcomeSummary(value));
    } else {
      body.textContent = value;
    }

    item.append(title, body);
    return item;
  }));
}

function renderOutcomeSummary(output) {
  const list = makeElement('div', null, 'summary-lines');
  const rounds = formatNumber(output.outcome?.numRounds ?? 0);
  const result = output.result;
  let message;

  if (result === 'AttackerWins') {
    message = `Attacker won in ${rounds} rounds.`;
  } else if (result === 'DefenderWins') {
    message = `Defender won in ${rounds} rounds.`;
  } else if (result === 'Draw') {
    message = 'Battle ended in a draw.';
  } else {
    message = 'Outcome unknown.';
  }

  list.replaceChildren(
    makeElement('div', message),
    makeElement('div', `Simulation time ${formatNumber(output.elapsedTime ?? 0)} ms.`),
  );
  return list;
}

function renderDebrisLines(output) {
  const list = makeElement('div', null, 'resource-lines');
  const debris = output.debris;
  const rows = [
    ['Metal', formatNumber(debris?.metal ?? 0)],
    ['Crystal', formatNumber(debris?.crystal ?? 0)],
    ['Moonchance', formatPercent(output.moonchance)],
  ];

  list.replaceChildren(...rows.map(([label, value]) => {
    const row = makeElement('div', null, 'resource-line');
    row.append(makeElement('span', `${label}:`), makeElement('span', value));
    return row;
  }));
  return list;
}

function renderResourceLines(resources) {
  const list = makeElement('div', null, 'resource-lines');
  const rows = [
    ['Metal', resources?.metal ?? 0],
    ['Crystal', resources?.crystal ?? 0],
    ['Deuterium', resources?.deuterium ?? 0],
  ];

  list.replaceChildren(...rows.map(([label, value]) => {
    const row = makeElement('div', null, 'resource-line');
    row.append(makeElement('span', `${label}:`), makeElement('span', formatNumber(value)));
    return row;
  }));
  return list;
}

async function loadUnits() {
  const response = await fetch('/sim-units');
  if (!response.ok) {
    throw new Error(await response.text());
  }

  const units = await response.json();
  renderUnitInputs(units);
  initializeSeed();
  prefillFormFromQuery();
  setStatus('');
}

async function runSimulation() {
  submitButton.disabled = true;
  setStatus('Running');
  const reportInput = collectReportInput();

  try {
    const response = await fetch(`/sim?${formToParams().toString()}`);
    const text = await response.text();

    if (!response.ok) {
      throw new Error(text);
    }

    const output = JSON.parse(text);
    renderSummary(output);
    renderReport(output, reportInput);
    setStatus('Complete');
  } catch (error) {
    setStatus(error.message || 'Simulation failed', true);
  } finally {
    randomizeSeed();
    submitButton.disabled = false;
  }
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  runSimulation();
});

form.addEventListener('reset', () => {
  window.setTimeout(() => {
    randomizeSeed();
    summaryEl.replaceChildren();
    reportEl.replaceChildren();
    setStatus('');
  });
});

loadUnits().catch((error) => {
  setStatus(error.message || 'Unable to load units', true);
});
