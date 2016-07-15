(function () {
    'use strict';

    angular
        .module('shrine.steward.login')
        .factory('LoginService', LoginService);

    LoginService.$inject = ['$http','StewardService'];
    function LoginService($http, StewardService) {

        var svc = StewardService;

        return {
            setAuthHeader: setAuthHeader,
            setCredentials: setCredentials,
            clearCredentials: clearCredentials
        };

        function setAuthHeader(username, password) {
            var authdata = svc.commonService.toBase64(username + ':' + password);
            $http.defaults.headers.common['Authorization'] =  'Basic ' + authdata;
        }

        function setCredentials(username, password, roles) {
            var authdata = svc.commonService.toBase64(username + ':' + password);
            svc.setAppUser(username, authdata, roles);
        }

        function clearCredentials() {
            var user = svc.getAppUser();
            svc.deleteAppUser();
            $http.defaults.headers.common.Authorization = ' Basic ';
        }
    }
})();
