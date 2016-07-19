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

        /**
         * -- set app user. --
         */
        function setAppUser(username, authdata, roles) {

            appUser = {
                username: username,
                authdata: authdata,
                isLoggedIn: true,
                roles: roles
            };
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
            var user = getAppUser();
            return (user.roles && user.roles.length) ?
                user.roles[0] : '';
        }
    }
})();
