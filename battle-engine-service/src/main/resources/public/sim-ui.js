const form = document.getElementById('sim-form');
const statusEl = document.getElementById('status');
const submitButton = document.getElementById('submit-button');
const summaryEl = document.getElementById('summary');
const rawOutputEl = document.getElementById('raw-output');
const attackerUnitsEl = document.getElementById('attacker-units');
const defenderUnitsEl = document.getElementById('defender-units');

function setStatus(message, isError = false) {
  statusEl.textContent = message;
  statusEl.classList.toggle('error', isError);
}

function unitInputName(side, index) {
  return `ship_${side}0_${index}_b`;
}

function renderUnitInputs(units) {
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

function resourceText(resources) {
  if (!resources) {
    return '0 / 0 / 0';
  }

  return `${resources.metal ?? 0} / ${resources.crystal ?? 0} / ${resources.deuterium ?? 0}`;
}

function renderSummary(output) {
  const rows = [
    ['Outcome', output.result ?? 'Unknown'],
    ['Debris', resourceText(output.debris)],
    ['Plunder', resourceText(output.possiblePlunder)],
    ['Attacker Losses', resourceText(output.lossesAttacker)],
    ['Defender Losses', resourceText(output.lossesDefender)],
  ];

  summaryEl.replaceChildren(...rows.map(([label, value]) => {
    const item = document.createElement('div');
    item.className = 'metric';

    const title = document.createElement('strong');
    title.textContent = label;

    const body = document.createElement('span');
    body.textContent = value;

    item.append(title, body);
    return item;
  }));
}

async function loadUnits() {
  const response = await fetch('/sim-units');
  if (!response.ok) {
    throw new Error(await response.text());
  }

  const units = await response.json();
  renderUnitInputs(units);
  setStatus('');
}

async function runSimulation() {
  submitButton.disabled = true;
  setStatus('Running');

  try {
    const response = await fetch(`/sim?${formToParams().toString()}`);
    const text = await response.text();

    if (!response.ok) {
      throw new Error(text);
    }

    const output = JSON.parse(text);
    renderSummary(output);
    rawOutputEl.textContent = JSON.stringify(output, null, 2);
    setStatus('Complete');
  } catch (error) {
    setStatus(error.message || 'Simulation failed', true);
  } finally {
    submitButton.disabled = false;
  }
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  runSimulation();
});

form.addEventListener('reset', () => {
  window.setTimeout(() => {
    summaryEl.replaceChildren();
    rawOutputEl.textContent = '{}';
    setStatus('');
  });
});

loadUnits().catch((error) => {
  setStatus(error.message || 'Unable to load units', true);
});
