angular
    .module('model-service', [])
    .constant("urlConfig", {
        base: "http://localhost:8080/steward/"
        //base: "https://shrine-qa1.hms.harvard.edu:6443/steward/"
    })
    .constant("mdlVerbs", {
        SKIP:           "{$SKIP$}",
        LIMIT:          "{$LIMIT$}",
        STATE:          "{$STATE$}",
        DIRECTION:      "{$DIRECTION$}",
        SORT_BY:        "{$SORT_BY$}",
        MIN_DT:         "{$MIN_DATE$}",
        MAX_DT:         "{$MAX_DATE$}",
        SKIP_PARAM:     "skip={$SKIP$}",
        LIMIT_PARAM:    "limit={$LIMIT$}",
        STATE_PARAM:    "state={$STATE$}",
        DIR_PARAM:      "sortDirection={$DIRECTION$}",
        SORT_BY_PARAM:  "sortBy={$SORT_BY$}",
        MIN_DT_PARAM:   "minDate={$MIN_DATE$}",
        MAX_DT_PARAM:   "maxDate={$MAX_DATE$}"
    })
    .constant("mdlStates", {
        STATE1: "Pending",
        STATE2: "Approved",
        STATE3: "Rejected"
    })
    .service('ModelService', ['mdlVerbs', 'mdlStates', '$location', function (verbs, states, $location) {

        var model    = this,
            absUrl   = $location.$$absUrl,
            key      = 'shrine-dashboard',
            url      = {
                base: "http://localhost:8080/dashboard-war/"
            },
            urlIdx;

        if (absUrl.indexOf('localhost') < 0) {
            urlIdx = absUrl.indexOf(key);
            url.base    = absUrl.substring(0, urlIdx) + 'shrine-dashboard/';
        }

        model.verbs  = verbs;
        model.url    = url;
        model.states = states;

        model.getParamString =  function (skip, limit, state, sortBy, sortDirection, minDate, maxDate) {
            var params = "";

            //@todo: make this a method, redundant if.
            if (skip !== undefined) {
                params += "?";
                params += verbs.SKIP_PARAM.replace(verbs.SKIP, skip);
            }

            if (limit !== undefined) {
                params += (params.length) ? "&" : "?";
                params += verbs.LIMIT_PARAM.replace(verbs.LIMIT, limit);
            }

            if (state !== undefined) {
                params += (params.length) ? "&" : "?";
                params += verbs.STATE_PARAM.replace(verbs.STATE, state);
            }

            if (sortBy !== undefined) {
                params += (params.length) ? "&" : "?";
                params += verbs.SORT_BY_PARAM.replace(verbs.SORT_BY, sortBy);
            }

            if (sortDirection !== undefined) {
                params += (params.length) ? "&" : "?";
                params += verbs.DIR_PARAM.replace(verbs.DIRECTION, sortDirection);
            }

            if (minDate !== undefined) {
                params += (params.length) ? "&" : "?";
                params += verbs.MIN_DT_PARAM.replace(verbs.MIN_DT, minDate);
            }

            if (maxDate !== undefined) {
                params += (params.length) ? "&" : "?";
                params += verbs.MAX_DT_PARAM.replace(verbs.MAX_DT, maxDate);
            }

            return params;
        };

        model.getURL =  function (url, skip, limit, state, sortBy, sortDirection, minUtc, maxUtc) {
            url += model.getParamString(skip, limit, state, sortBy, sortDirection, minUtc, maxUtc);
            return url;
        };

        model.getRoleSegment  = function (role, appRoles) {
            return (role === appRoles.ROLE1) ? "researcher" : "steward";
        };

        model.isIDEA = function () {
            return Boolean(absUrl.indexOf('localhost') >= 0);
        };

    }]);

