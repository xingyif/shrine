(function () {

    // -- register directive with angular -- //
    angular.module('shrine-tools')
        .directive('userStatus', UserStatus);

    /***
     *
     * @returns {{restict: string, replace: boolean, templateUrl: string,
     * controller: UserStatusController, controllerAs: string, link: UserStatusLinker, scope: {}}}
     * @constructor
     */
    function UserStatus () {
        return {
            restict:        'E',
            replace:        true,
            templateUrl:    'src/app/header/user-status.tpl.html',
            controller:     UserStatusController,
            controllerAs:   'vm',
            link:           UserStatusLinker,
            scope: {
            }
        }
    }


    /**
     *
     * @type {string[]}
     */
    UserStatusController.$inject = ['$location', '$app'];
    function UserStatusController ($location, $app) {
        var vm = this;

        vm.isUserLoggedIn   = isUserLoggedIn;
        vm.getUsername      = getUsername;
        vm.logout           = logout;


        /**
         *
         * @returns {boolean}
         */
        function isUserLoggedIn () {
            return ($app.globals.currentUser !== undefined && $app.globals.currentUser.isLoggedIn === true);
        }


        /**
         *
         * @returns {*}
         */
        function getUsername () {
            return ($app.globals.currentUser) ?
                $app.globals.currentUser.username : '';
        }


        /**
         *
         *
         */
        function logout () {
            $location.path('/login');
        }
    }


    /**
     *
     * @type {string[]}
     */
    UserStatusLinker.$inject = ['scope', 'element', 'attributes'];
    function UserStatusLinker (s, e, a) {
        var vm = s.vm;
    }

})();


