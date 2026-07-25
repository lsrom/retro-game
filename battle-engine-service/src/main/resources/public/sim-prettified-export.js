(function () {
  const DEFAULT_TEMPLATE_URL = '/sim-prettified-export-template.html';
  const FALLBACK_EXPORT_TEMPLATES = [
    { label: 'Table', templateUrl: DEFAULT_TEMPLATE_URL },
    { label: 'Classic', templateUrl: '/sim-prettified-export-classic-template.html' },
    { label: 'Roster', templateUrl: '/sim-prettified-export-roster-template.html' },
  ];

  function combatantForExport(combatant) {
    return {
      title: combatant.title,
      coordinates: combatant.coordinates,
      weaponsTechnology: combatant.weaponsTechnology,
      shieldingTechnology: combatant.shieldingTechnology,
      armorTechnology: combatant.armorTechnology,
      unitGroups: Object.fromEntries(combatant.unitGroups ?? []),
    };
  }

  async function downloadResponse(response, output) {
    const html = await response.text();
    if (!response.ok) {
      throw new Error(html || response.statusText);
    }

    const blob = new Blob([html], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `combat-report-${output.outcome?.seed ?? 'sim'}.html`;
    document.body.append(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  window.exportPrettifiedReport = async function exportPrettifiedReport(output, input, units, templateUrl = DEFAULT_TEMPLATE_URL) {
    const response = await fetch('/sim-prettified-export', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        output,
        input: {
          attacker: combatantForExport(input.attacker),
          defender: combatantForExport(input.defender),
        },
        templateUrl,
      }),
    });

    await downloadResponse(response, output);
  };

  window.prettifiedReportTemplates = FALLBACK_EXPORT_TEMPLATES;

  fetch('/sim-prettified-export-templates')
    .then(async (response) => {
      if (response.ok) {
        window.prettifiedReportTemplates = await response.json();
      }
    })
    .catch(() => {});
}());
