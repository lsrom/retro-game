'use strict';

function prettyNumber(n) {
  var lang = document.documentElement.lang || 'en';
  return n.toLocaleString(lang);
}

function prettyDate(t) {
  var date = new Date(t * 1000);
  var now = new Date();
  var s = '';
  if (now.getDate() !== date.getDate() || now.getMonth() !== date.getMonth() || now.getFullYear() !== date.getFullYear()) {
    s += date.getFullYear() + '-' + ('0' + (date.getMonth() + 1)).slice(-2) + '-' + ('0' + date.getDate()).slice(-2) + ' ';
  }
  return s + ('0' + date.getHours()).slice(-2) + ':' + ('0' + date.getMinutes()).slice(-2) + ':' + ('0' + date.getSeconds()).slice(-2);
}

function prettyTime(t) {
  var seconds = t % 60;
  t = Math.floor(t / 60);
  var minutes = t % 60;
  t = Math.floor(t / 60);
  var hours = t % 24;
  var days = Math.floor(t / 24);
  return (days > 0 ? days + ' d ' : '') + ('0' + hours).slice(-2) + ':' + ('0' + minutes).slice(-2) + ':' + ('0' + seconds).slice(-2);
}

function getResources() {
  var names = ['metal', 'crystal', 'deuterium'];
  var resources = {};
  for (var i = 0; i < names.length; i++) {
    var name = names[i];
    resources[name] = +$('[data-resources-' + name + ']').attr('data-resources-' + name);
  }
  return resources;
}

$(function () {
  if ($(document.body).attr('data-number-input-scrolling') === 'false') {
    $(document).on("wheel", "input[type=number]", function () {
      $(this).blur();
    });
  }

  $('#top-bar-body-list :input').change(function () {
    $('#top-bar-body-list').submit();
  });

  var timers = $('[data-timer]');
  if (timers.length > 0) {
    var updateTimers = function () {
      var now = new Date().getTime() / 1000 | 0;
      for (var i = 0; i < timers.length; i++) {
        var timer = timers[i];
        var t = +$(timer).attr('data-timer');
        var diff = t - now;
        timer.innerHTML = (diff > 0 ? prettyTime(diff) : '-') + '<br><span class="date">' + prettyDate(t) + '</span>';
      }
    };
    updateTimers();
    setInterval(updateTimers, 100);
  }

  // Live-ticking resources: the top bar shows each body's metal, crystal and
  // deuterium stockpiles only as rendered at page load. To save the player
  // from reloading, increment the displayed numbers once a second from the
  // body's hourly production, clamped between zero and storage capacity
  // (production halts once a store is full).
  var resourceBar = $('#top-bar-resources');
  if (resourceBar.length > 0) {
    var resourceNames = ['metal', 'crystal', 'deuterium'];
    var loadedAt = Date.now();
    var resourceBase = {}, resourceProduction = {}, resourceCapacity = {};
    for (var i = 0; i < resourceNames.length; i++) {
      var name = resourceNames[i];
      resourceBase[name] = +resourceBar.attr('data-resources-' + name);
      resourceProduction[name] = +resourceBar.attr('data-production-' + name);
      resourceCapacity[name] = +resourceBar.attr('data-capacity-' + name);
    }
    var updateResources = function () {
      var elapsedHours = (Date.now() - loadedAt) / 3600000;
      for (var i = 0; i < resourceNames.length; i++) {
        var name = resourceNames[i];
        var capacity = resourceCapacity[name];
        var current = resourceBase[name] + resourceProduction[name] * elapsedHours;
        current = Math.max(0, Math.min(current, capacity));
        var cell = $('[data-resource-value="' + name + '"]');
        cell.text(prettyNumber(Math.floor(current)));
        // Keep the capacity warning in sync as the value ticks: red once a
        // store is full, amber from 90% so the player can react in time.
        cell.toggleClass('no-capacity', current >= capacity);
        cell.toggleClass('almost-full', current >= capacity * 0.9 && current < capacity);
      }
    };
    updateResources();
    setInterval(updateResources, 1000);
  }

  // Surface unread reports + messages in the browser tab title (e.g.
  // "(3) Overview") so the player notices activity from another tab.
  var sidebar = $('#sidebar');
  if (sidebar.length > 0) {
    var unreadCount = +sidebar.attr('data-unread-count');
    if (unreadCount > 0) {
      document.title = '(' + unreadCount + ') ' + document.title;
    }
  }

  $('[data-set]').click(function () {
    var input = $('#' + $(this).attr('data-set-for'));
    input.val($(this).attr('data-set-value'));
    input.trigger('change');
    return false;
  });

  $('[data-tooltip]').each(function () {
    var self = $(this);
    new Tooltip(this, {
      html: self.attr('data-tooltip-html') !== undefined,
      title: self.attr('data-tooltip-title')
    })
  });
});
