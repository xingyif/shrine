
(function () {
    angular
        .module('shrine-tools')
        .factory("$app", DiagnosticService);

    DiagnosticService.$inject = ['UtilsService', 'DiagnosticModel'];
    function DiagnosticService(utilsService, diagnosticModel) {

        var _app = {
            globals: {
            },
            utils:   extendUtils(utilsService),
            model:   diagnosticModel
        };

        var m = true;
        _app.m = true;

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

