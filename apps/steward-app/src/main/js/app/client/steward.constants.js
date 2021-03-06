(function () {
  'use strict';

  var server = 'http://localhost';
  var restBase = 'steward/';
  var port = ':6443/';
  var baseUrl = server + port + restBase;
  var homeRoute = '/researcher/topics';
  var defaultRoute = '/login';

  var restOptions = {
    skip: '{$SKIP$}',
    limit: '{$LIMIT$}',
    state: '{$STATE$}',
    direction: '{$DIRECTION$}',
    sortBy: '{$SORT_BY$}',
    minDate: '{$MIN_DATE$}',
    maxDate: '{$MAX_DATE$}',
  };

  var restInterpolators = {
    skip: 'skip=' + restOptions.skip,
    limit: 'limit=' + restOptions.limit,
    state: 'state=' + restOptions.state,
    direction: 'sortDirection=' + restOptions.direction,
    sortBy: 'sortBy=' + restOptions.sortBy,
    minDate: 'minDate=' + restOptions.minDate,
    maxDate: 'maxDate=' + restOptions.maxDate
  };

  var states = {
    state1: 'Pending',
    state2: 'Approved',
    state3: 'Rejected'
  };

  var viewConfig = {
    index: 1,
    range: 4,
    limit: 20
  };

  // -- todo: delete?
  var title = 'SHRINE DATA STEWARD';

  var roles = {
    researcher: 'Researcher',
    dataSteward: 'DataSteward',
    admin: 'Admin'
  };

  angular
    .module('shrine.steward')
    .constant('constants', {
      'homeRoute': homeRoute,
      'defaultRoute': defaultRoute,
      'baseUrl': baseUrl,
      'restOptions': restOptions,
      'restInterpolators': restInterpolators,
      'states': states,
      'roles': roles,
      'title': title,
      'viewConfig': viewConfig
    });
})();

