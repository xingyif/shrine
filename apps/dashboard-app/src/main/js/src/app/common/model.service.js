
(function() {
    'use strict'

    var verbs = {
        SKIP:           "{$SKIP$}",
        LIMIT:          "{$LIMIT$}",
        STATE:          "{$STATE$}",
        DIRECTION:      "{$DIRECTION$}",
        SORT_BY:        "{$SORT_BY$}",
        MIN_DATE:       "{$MIN_DATE$}",
        MAX_DATE:       "{$MAX_DATE$}",
    }

    var params = {
        SKIP:     "skip={$SKIP$}",
        LIMIT:    "limit={$LIMIT$}",
        STATE:    "state={$STATE$}",
        DIRECTION:      "sortDirection={$DIRECTION$}",
        SORT_BY:  "sortBy={$SORT_BY$}",
        MIN_DATE:   "minDate={$MIN_DATE$}",
        MAX_DATE:   "maxDate={$MAX_DATE$}"
    }

    /**
     *
     * @param verbs
     * @param $location
     * @constructor
     */
    function ModelService($location) {
        // -- scope -//
        var svc = this;

        // -- public vars --//
        svc.params  = params;
        svc.verbs   = verbs;
        svc.url     = init('url');

        // -- public methods -- //
        svc.getParamString  = getParamString;
        svc.getURL          = getURL;
        svc.isIDEA          = isIDEA;

        // -- private methods -- //
        /**
         *
         * @param skip
         * @param limit
         * @param state
         * @param sortBy
         * @param sortDirection
         * @param minDate
         * @param maxDate
         */
        function getParamString (skip, limit, state, sortBy, sortDirection, minDate, maxDate) {
            return setVerbValue(verbs.SKIP, skip, params.SKIP) +
                    setVerbValue(verbs.LIMIT, limit, params.LIMIT) +
                    setVerbValue(verbs.STATE, state, params.STATE) +
                    setVerbValue(verbs.SORT_BY, sortBy, params.SORT_BY) +
                    setVerbValue(verbs.DIRECTION, sortDirection. params.DIRECTION) +
                    setVerbValue(verbs.MIN_DATE, minDate, params.MIN_DATE) +
                    setVerbValue(verbs.MAX_DATE, maxDate, params.MAX_DATE);
        }

        /**
         *
         * @param url
         * @param skip
         * @param limit
         * @param state
         * @param sortBy
         * @param sortDirection
         * @param minUtc
         * @param maxUtc
         * @returns {*}
         */
        function getURL(url, skip, limit, state, sortBy, sortDirection, minUtc, maxUtc) {
            url += getParamString(skip, limit, state, sortBy, sortDirection, minUtc, maxUtc);
            return url;
        }

        /**
         *
         * @returns {boolean}
         */
        function isIDEA() {
            return Boolean(svc.url.base.indexOf('localhost') >= 0);
        };

        /**
         *
         * @param verbString
         * @param value
         * @param paramString
         * @returns {string}
         */
        function setVerbValue (verbString, value, paramString) {
            var queryString = ""

            if (value !== undefined) {
                queryString += "?";
                queryString += paramString.replace(verbString, skip);
            }

            return queryString
        }

        //-- private vars --//
        var initMap =  undefined,
            absUrl  = $location.$$absUrl,
            key     = 'shrine-dashboard',
            url     = {},
            urlIdx  = 0;

        function init(key) {

            if(initMap === undefined) {
                // -- set url -- //
                urlIdx   = absUrl.indexOf(key);
                url.base = absUrl.substring(0, urlIdx) + key + '/';

                // -- for local testing --//
                if (absUrl.indexOf('localhost') > -1) {
                    url.base = 'http://localhost:8080/dashboard-war/'
                }

                // -- set init obj --//
                initMap = {
                    url: url
                }
            }

            return initMap[key];
        }
    }
})()
