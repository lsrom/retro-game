'use strict';

$(function () {
  var inputs = $('[data-building-time-ms]');

  function updateTotals(inputElement) {
    var input = $(inputElement);
    var count = Math.floor(+input.val());
    var total = input.closest('.item').find('.shipyard-total-building-time');
    var resourceTotal = input.closest('.item').find('.total-resource-cost');

    if (!isFinite(count) || count < 1) {
      total.attr('hidden', true);
      resourceTotal.attr('hidden', true);
      return;
    }

    var buildingTimeMs = +input.attr('data-building-time-ms');
    total.find('[data-total-building-time]').text(prettyTime(Math.floor(buildingTimeMs * count / 1000)));
    total.removeAttr('hidden');

    var resourceBar = $('#top-bar-resources');
    resourceTotal.find('[data-total-resource-cost]').each(function () {
      var output = $(this);
      var resource = output.attr('data-total-resource-cost');
      var unitCost = +input.closest('.item').find('[data-resource-cost="' + resource + '"]')
        .attr('data-resource-cost-value');
      var cost = unitCost * count;
      var current = +resourceBar.attr('data-current-resources-' + resource);

      output.text(prettyNumber(cost));
      output.toggleClass('requirement-met', current >= cost);
      output.toggleClass('requirement-not-met', current < cost);
    });
    resourceTotal.removeAttr('hidden');
  }

  inputs.on('input change', function () {
    updateTotals(this);
  }).trigger('change');

  $('#top-bar-resources').on('resources-updated', function () {
    inputs.each(function () {
      updateTotals(this);
    });
  });
});
