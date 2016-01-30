
(function () {
    angular
        .module('shrine-tools')
        .factory("$app", DiagnosticService);

    DiagnosticService.$inject = ['AppUtilsService', 'DiagnosticModel']
    function DiagnosticService(AppUtilsService, DiagnosticModel) {

        var _app = {
            globals: {
            },
            utils:   extendUtils(AppUtilsService),
            model:   DiagnosticModel
        };

        return _app;


        // --  private -- //
        /**
         *
         * @param svc
         * @returns {*}
         */
        function extendUtils (svc) {
            svc.setAppUser      = setAppUser;
            svc.deleteAppUser   = deleteAppUser;
            return svc;
        }


        /**
         *
         * @param username
         * @param authdata
         * @param roles
         */
        function setAppUser(username, authdata, roles) {

            var userRoles = (roles.length)? [] : [roles[roles.length -1]];

            _app.globals.currentUser = {
                username:   username,
                authdata:   authdata,
                isLoggedIn: true,
                roles:      userRoles
            };
        };


        /**
         *
         */
        function deleteAppUser() {
            delete _app.globals.currentUser;
        }
    }
})();

