(function () {
    'use strict';
    angular
        .module('shrine.common')
        .factory('ModelService', ModelService);

    var interpolators = {
        skip: '{$SKIP$}',
        limit: '{$LIMIT$}',
        state: '{state$}',
        direction: '{$DIRECTION$}',
        sortBy: '{$SORT_BY$}',
        minDate: '{$MIN_DATE$}',
        maxDate: '{$MAX_DATE$}'
    };

    var options = {
        skipParam: 'skip=' + interpolators.skip,
        limitParam: 'limit=' + interpolators.limit,
        stateParam: 'state=' + interpolators.state,
        dirParam: 'sortDirection=' + interpolators.direction,
        sortByParam: 'sortBy=' + interpolators.sortBy,
        minDateParam: 'minDate=' + interpolators.minDate,
        maxDateParam: 'maxDate=' + interpolators.minDate,
    };

    //-- steward specific logic.  move to steward base.
    var states = {
        state1: 'Pending',
        state2: 'Approved',
        state3: 'Rejected'
    };

    ModelService.$inject = [''];
    function ModelService() {

        return {
            // -- constants -- //
            interpolators: interpolators,
            options: options,
            states: states,

            // -- methods -- //
            isTest: isTest,
            getParamString: getParamString,
            getRoleSegment: getRoleSegment,
            getURL: getURL
        };

        function getParamString(skip, limit, state, sortBy, sortDirection, minDate, maxDate) {

            var params = '';

            function addToParams(value, interpolator, option) {
                params += (params.length) ? '&' : '?';
                params += option.replace(interpolator, value);
            }

            if (skip) {
                addToParams(skip);
            }

            if (limit) {
                addToParams(limit);
            }

            if (state) {
                addToParams(state);
            }

            if (sortBy) {
                addToParams(sortBy);
            }

            if (sortDirection) {
                addToParams(sortDirection);
            }

            if (minDate) {
                addToParams(minDate);
            }

            if (maxDate) {
                addToParams(maxDate);
            }

            return params;
        }

        //consider moving port number to steward.service.
        function isTest(url) {
            var portNumber = 63342;
            url = url || document.URL;
            return !!(url.indexOf('localhost:' + portNumber) > -1);
        }

        function getURL(url, skip, limit, state, sortBy, sortDirection, minUtc, maxUtc) {
            url += getParamString(skip, limit, state, sortBy, sortDirection, minUtc, maxUtc);
            return url;
        }

        // -- todo, move this to steward.service.js
        function getRoleSegment(role, appRoles) {
            return (role === appRoles.ROLE1) ? 'researcher' : 'steward';
        }
    }
})();
