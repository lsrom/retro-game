'use strict';

$(document).on('keydown', function (event) {
  switch (event.which) {
    case 40: // down arrow
      $('#galaxy-prev').submit();
      break;
    case 38: // up arrow
      $('#galaxy-next').submit();
      break;
    case 37: // left arrow
      $('#system-prev').submit();
      break;
    case 39: // right arrow
      $('#system-next').submit();
      break;
    case 32: // space
      var reload = $('#galaxy-reload');
      if (reload !== undefined) {
        $('form', reload).submit();
      }
      break;
  }
});

function galaxySubmit() {
  let galaxy = 0 | $('#galaxy-input [name="galaxy"]').val();
  if (galaxy < 1 || galaxy > 5)
    galaxy = 1;
  $('[name="galaxy"]', this).val(galaxy);

  let system = 0 | $('#system-input [name="system"]').val();
  if (system < 1 || system > 500)
    system = 1;
  $('[name="system"]', this).val(system);
}

$('#galaxy-input').submit(galaxySubmit);
$('#system-input').submit(galaxySubmit);

function addGalaxyReport(html) {
  let reports = $('#reports');
  if (reports.children().length === 0) {
    reports.append('<tr><th>Reports</th></tr>');
  }
  $('#reports :first-child').first().after(html);
}

function formatCoordinates(galaxy, system, position, kind) {
  return [galaxy, system, position, kind[0]].join('-');
}

function formatUnits(units) {
  return Object.keys(units)
    .map(function (kind) {
      return units[kind] + ' ' + kind.replace(/_/g, ' ').toLowerCase();
    })
    .join(', ');
}

$('[data-spy]').click(function () {
  const body = 0 | $($('[name="body"]')[0]).val();
  const galaxy = 0 | $(this).attr('data-galaxy');
  const system = 0 | $(this).attr('data-system');
  const position = 0 | $(this).attr('data-position');
  const kind = $(this).attr('data-kind');
  const count = 0 | $('#num-probes').val();

  $.ajax({
    type: 'post',
    url: '/flights/send-probes',
    contentType: 'application/json',
    data: JSON.stringify({
      body: body,
      galaxy: galaxy,
      system: system,
      position: position,
      kind: kind,
      count: count
    }),
    success: function (data) {
      const coordinates = formatCoordinates(galaxy, system, position, kind);
      if (data.success) {
        addGalaxyReport('<tr><td>Probes were sent to ' + coordinates + ' successfully</td></tr>');
        return;
      }
      let message = 'Probes couldn\'t be sent to ' + coordinates + ', ';
      switch (data.error) {
        case 'NO_MORE_FREE_SLOTS':
          message += 'no more free flight slots';
          break;
        case 'NOT_ENOUGH_CAPACITY':
          message += 'the target is too far away';
          break;
        case 'NOT_ENOUGH_DEUTERIUM':
          message += 'you don\'t have enough fuel';
          break;
        case 'NOT_ENOUGH_UNITS':
          message += 'you don\'t have enough probes';
          break;
        case 'CONCURRENCY':
          message += 'please try again'
          break;
      }
      addGalaxyReport('<tr><td><font color="red">' + message + '</font></td></tr>');
    },
    error: function () {
      addGalaxyReport('<tr><td><font color="red">Internal error</font></td></tr>');
    }
  });
});

$('[data-attack-again]').click(function () {
  const body = 0 | $($('[name="body"]')[0]).val();
  const galaxy = 0 | $(this).attr('data-galaxy');
  const system = 0 | $(this).attr('data-system');
  const position = 0 | $(this).attr('data-position');
  const kind = $(this).attr('data-kind');

  $.ajax({
    type: 'post',
    url: '/flights/attack-again',
    contentType: 'application/json',
    data: JSON.stringify({
      body: body,
      galaxy: galaxy,
      system: system,
      position: position,
      kind: kind
    }),
    success: function (data) {
      const coordinates = formatCoordinates(galaxy, system, position, kind);
      if (data.success) {
        addGalaxyReport('<tr><td>Attack launched to ' + coordinates + ': ' + formatUnits(data.units) + '</td></tr>');
        return;
      }
      let message = 'Attack couldn\'t be launched to ' + coordinates + ', ';
      switch (data.error) {
        case 'NO_MORE_FREE_SLOTS':
          message += 'no more free flight slots';
          break;
        case 'NOT_ENOUGH_CAPACITY':
          message += 'the target is too far away';
          break;
        case 'NOT_ENOUGH_DEUTERIUM':
          message += 'you don\'t have enough fuel';
          break;
        case 'NOT_ENOUGH_UNITS':
          message += 'you don\'t have enough ships';
          break;
        case 'REPORT_DOES_NOT_EXIST':
          message += 'there is no repeatable combat report';
          break;
        case 'BODY_DOES_NOT_EXIST':
          message += 'there is no planet at the target coordinates';
          break;
        case 'NOOB_PROTECTION':
          message += 'the target player is too weak or too strong';
          break;
        case 'TARGET_ON_VACATION':
          message += 'the target player is on vacation';
          break;
        case 'WRONG_TARGET':
        case 'WRONG_TARGET_KIND':
        case 'WRONG_TARGET_USER':
          message += 'wrong target';
          break;
        case 'CONCURRENCY':
          message += 'please try again';
          break;
        default:
          message += 'unknown error';
          break;
      }
      addGalaxyReport('<tr><td><font color="red">' + message + '</font></td></tr>');
    },
    error: function () {
      addGalaxyReport('<tr><td><font color="red">Internal error</font></td></tr>');
    }
  });
});
