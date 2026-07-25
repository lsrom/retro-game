const statusEl = document.getElementById('status');
const historyBody = document.getElementById('history-body');
const previousButton = document.getElementById('previous-button');
const nextButton = document.getElementById('next-button');
const pageLabel = document.getElementById('page-label');
const importButton = document.getElementById('import-button');
const exportButton = document.getElementById('export-button');
const importFileInput = document.getElementById('import-file-input');
const numberFormatter = new Intl.NumberFormat();
const dateFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'medium',
});

const pageSize = 100;
let offset = Number.parseInt(new URLSearchParams(window.location.search).get('offset'), 10) || 0;
let currentItems = [];

function setStatus(message, isError = false) {
  statusEl.textContent = message;
  statusEl.classList.toggle('error', isError);
}

function formatNumber(value) {
  return numberFormatter.format(Math.round(Number(value) || 0));
}

function formatDate(timestamp) {
  return dateFormatter.format(new Date(Number(timestamp)));
}

function formatEngine(engine) {
  if (engine === 'java') {
    return 'Java';
  }
  if (engine === 'native') {
    return 'Native';
  }
  return '';
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

function updatePager() {
  previousButton.disabled = offset === 0;
  nextButton.disabled = currentItems.length < pageSize;
  pageLabel.textContent = `${formatNumber(offset + 1)}-${formatNumber(offset + currentItems.length)}`;
}

function replayHref(item) {
  return item.query ? `/sim-ui?${item.query}` : '/sim-ui';
}

async function copyQuery(query) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(query);
    return;
  }

  const textArea = document.createElement('textarea');
  textArea.value = query;
  textArea.style.position = 'fixed';
  textArea.style.left = '-9999px';
  document.body.append(textArea);
  textArea.focus();
  textArea.select();
  try {
    document.execCommand('copy');
  } finally {
    textArea.remove();
  }
}

function renderRow(item, index) {
  const row = document.createElement('tr');
  row.append(
    makeElement('td', formatNumber(offset + index + 1)),
    makeElement('td', formatDate(item.utcTimestamp)),
    makeElement('td', formatNumber(item.seed)),
    makeElement('td', formatEngine(item.engine)),
    makeElement('td', formatNumber(item.totalAttackerLosses)),
    makeElement('td', formatNumber(item.totalDefenderLosses)),
    makeElement('td', formatNumber(item.totalDebrisField)),
    makeElement('td', formatNumber(item.plunder)),
    makeElement('td', `${formatNumber(item.elapsedTime)} ms`),
  );

  const actionCell = makeElement('td', null, 'row-actions');
  const copyButton = makeElement('button', 'Copy', 'secondary');
  copyButton.type = 'button';
  copyButton.addEventListener('click', async () => {
    try {
      await copyQuery(item.query || '');
      setStatus('Copied');
    } catch (error) {
      setStatus(error.message || 'Unable to copy query', true);
    }
  });
  const link = makeElement('a', 'View');
  link.href = replayHref(item);
  link.className = 'button secondary';
  actionCell.append(copyButton, link);
  row.append(actionCell);

  return row;
}

function renderList(items) {
  currentItems = items;
  if (items.length === 0) {
    const row = document.createElement('tr');
    const cell = makeElement('td', 'No simulations found.');
    cell.colSpan = 10;
    row.append(cell);
    historyBody.replaceChildren(row);
  } else {
    historyBody.replaceChildren(...items.map(renderRow));
  }
  updatePager();
}

async function fetchJson(url) {
  const response = await fetch(url);
  const text = await response.text();
  if (!response.ok) {
    throw new Error(text || response.statusText);
  }
  return JSON.parse(text);
}

async function loadList() {
  setStatus('Loading');
  try {
    const items = await fetchJson(`/sim-history?limit=${pageSize}&offset=${offset}`);
    renderList(items);
    setStatus('');
  } catch (error) {
    setStatus(error.message || 'Unable to load history', true);
  }
}

async function importCsv(file) {
  const formData = new FormData();
  formData.set('file', file);

  setStatus('Importing');
  const response = await fetch('/sim-history/import', {
    method: 'POST',
    body: formData,
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(text || response.statusText);
  }

  const result = JSON.parse(text);
  offset = 0;
  await loadList();
  setStatus(`Imported ${formatNumber(result.imported)} rows`);
}

previousButton.addEventListener('click', () => {
  offset = Math.max(0, offset - pageSize);
  loadList();
});

nextButton.addEventListener('click', () => {
  offset += pageSize;
  loadList();
});

exportButton.addEventListener('click', () => {
  window.location.href = '/sim-history/export';
});

importButton.addEventListener('click', () => {
  importFileInput.click();
});

importFileInput.addEventListener('change', async () => {
  const file = importFileInput.files?.[0];
  importFileInput.value = '';
  if (!file) {
    return;
  }

  try {
    await importCsv(file);
  } catch (error) {
    setStatus(error.message || 'Unable to import CSV', true);
  }
});

loadList();
