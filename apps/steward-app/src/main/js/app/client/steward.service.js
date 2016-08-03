(function () {
    'use strict';
    angular
        .module('shrine.steward')
        .provider('StewardService', StewardProvider);

    StewardProvider.$inject = ['constants'];
    function StewardProvider(constants) {

        // -- make available to configuration --//
        this.$get = get;
        this.configureHttpProvider = configureHttpProvider;
        this.constants = constants;

        // -- provide steward service --//
        get.$inject = ['CommonService'];
        function get(CommonService) {
            return new StewardService(CommonService, constants);
        }

        /**
         * Set up cross domain voodoo, if running from deployment, No IE Cache
         * @param httpProvider
         * @returns {*}
         * @see: http://stackoverflow.com/questions/16098430/angular-ie-caching-issue-for-http
         */
        function configureHttpProvider(httpProvider) {

            // -- set up cross domain -- //
            httpProvider.defaults.useXDomain = true;
            delete httpProvider.defaults.headers.common['X-Requested-With'];
            httpProvider.defaults.headers.common['Access-Control-Allow-Headers'] = '*';


            // -- If running from deployment, No IE Cache -- //
            if (window.location.origin.indexOf('http://localhost:') === -1) {

                //initialize get if not there
                if (!httpProvider.defaults.headers.get) {
                    httpProvider.defaults.headers.get = {};
                }
                //disable IE ajax request caching
                httpProvider.defaults.headers.get['If-Modified-Since'] = 'Sat, 26 Jul 1997 05:00:00 GMT';
                httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache';
                httpProvider.defaults.headers.get['Pragma'] = 'no-cache';
            }

            return httpProvider;
        }
    }

    /**
     * Steward Servcice.
     */
    function StewardService(CommonService, constants) {

        // -- private vars -- //
        var appTitle = null;
        var appUser = null;
        var loginSubscriber;

        // -- public members -- //
        this.commonService = CommonService;
        this.constants = constants;

        // -- public methods -- //
        this.setAppUser = setAppUser;
        this.getAppUser = getAppUser;
        this.deleteAppUser = deleteAppUser;
        this.isUserLoggedIn = isUserLoggedIn;
        this.getUsername = getUsername;
        this.getRole = getRole;
        this.getUrl = getUrl;
        this.isSteward = isSteward;
        this.setLoginSubscriber = setLoginSubscriber;

        function setLoginSubscriber(subscriber) {
            loginSubscriber = subscriber;
        }

        /**
         * -- set app user. --
         */
        function setAppUser(username, authdata, roles) {

            var primaryRole = (roles.length > 1) ?
                constants.roles.dataSteward : constants.roles.researcher;

            appUser = {
                username: username,
                authdata: authdata,
                isLoggedIn: true,
                role: primaryRole
            };

            if (loginSubscriber) {
                loginSubscriber(appUser);
            }
        }

        /**
         * -- read only --
         */
        function getAppUser() {
            return angular.extend({}, appUser);
        }

        /**
         * -- delete user --
         */
        function deleteAppUser() {
            appUser = null;
        }

        function isUserLoggedIn() {
            return getAppUser()
                .isLoggedIn === true;
        }

        function getUsername() {
            return getAppUser()
                .username;
        }

        function getRole() {
            return getAppUser()
                .role;
        }

        function isSteward() {
            return getRole() === constants.roles.dataSteward;
        }

        function getUrl(restSegment, skip, limit, state, sortBy, sortDirection, minDate, maxDate) {
            restSegment = restSegment || '';
            var url = getDeployUrl('steward') + restSegment +
                getQueryString(skip, limit, state, sortBy, sortDirection, minDate, maxDate);

            return url;
        }

        function getQueryString(skip, limit, state, sortBy, sortDirection, minDate, maxDate) {
            var restOptions = constants.restOptions;
            var restInterpolators = constants.restInterpolators;
            var queryString = '';

            //todo: refactor this repetition.
            queryString = interpolateQueryStringValue(queryString, skip, restInterpolators.skip, restOptions.skip);
            queryString = interpolateQueryStringValue(queryString, limit, restInterpolators.limit, restOptions.limit);
            queryString = interpolateQueryStringValue(queryString, state, restInterpolators.state, restOptions.state);
            queryString = interpolateQueryStringValue(queryString, sortBy, restInterpolators.sortBy, restOptions.sortBy)
            queryString = interpolateQueryStringValue(queryString, sortDirection, restInterpolators.direction, restOptions.direction);
            queryString = interpolateQueryStringValue(queryString, minDate, restInterpolators.minDate, restOptions.minDate);
            queryString = interpolateQueryStringValue(queryString, maxDate, restInterpolators.maxDate, restOptions.maxDate);

            return queryString;
        }

        function interpolateQueryStringValue(queryString, value, interpolator, option) {

            if (value !== null && value !== undefined) {
                queryString += (queryString.length > 1) ? '&' : '?';
                queryString += interpolator.replace(option, value);
            }
            return queryString;
        }



        /**
         *
         * @param urlKey
         * @returns baseUrl of current site or baseUrl specified in steward.constants.
         */
        function getDeployUrl(urlKey) {

            // -- local vars. -- //
            var startIndex = 0, urlIndex = 0;
            var href = '';

            //no DOM, abandon ship!
            if (!document) {
                return constants.baseUrl;
            }

            href = document.location.href;
            startIndex = href.indexOf(urlKey);

            // -- wrong url, abandon ship! --//
            if (startIndex < 0) {
                return constants.baseUrl;
            }

            // -- parse url from location.
            urlIndex = startIndex + urlKey.length;
            return href.substring(0, urlIndex) + '/';
        }
    }
})();
