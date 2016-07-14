(function () {
  'use strict';

  var baseUrl = 'https://localhost:6443/steward/';
  var testPort = '8000';
  var defaultRoute = '/topics';

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

  // -- todo: delete?
  var title = 'SHRINE DATA STEWARD';

  var roles = {
    role1: 'researcher',
    role2: 'data-steward',
    role3: 'admin'
  };

  angular
    .module('shrine.steward')
    .constant('constants', {
      'defaultRoute': defaultRoute,
      'baseUrl': baseUrl,
      'restOptions': restOptions,
      'restInterpolators': restInterpolators,
      'states': states,
      'roles': roles,
      'testPort': testPort,
      'title': title//todo: delete?
    });
})();

