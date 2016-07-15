(function () {
    'use strict';

    describe('shrine.steward.login LoginService tests', LoginServiceSpec);

    function LoginServiceSpec() {

        // -- vars -- //
        var loginService, http, stewardService;
        // -- arrange -- //
        var username = 'test';
        var password = 'testpassword';
        var roles = ['testRole1', 'testRole2', 'testRole3'];

        function setup() {
            module('shrine.steward.login');
            inject(function (LoginService, $http, StewardService) {
                loginService = LoginService;
                http = $http;
                stewardService = StewardService;
            });
        }

        //-- setup --/
        beforeEach(setup);

        // -- tests -- //
        it('LoginService should exist', function () {
            expect(typeof (loginService)).toBe('object');
        });

        it('setAuthHeader should set the Authorization Header', function () {

            // -- arrange -- //
            var expectedAuthHeader = 'Basic dGVzdDp0ZXN0cGFzc3dvcmQ=';

            //-- act --//
            loginService.setAuthHeader(username, password);
            var result = http.defaults.headers.common['Authorization'];

            // -- assert --//
            expect(result).toEqual(expectedAuthHeader);
        });

        it('setCredentials should set the proper user credentials', function () {

            // -- arrange -- //
            var expectedResult = {
                username: 'test',
                authdata: 'dGVzdDp0ZXN0cGFzc3dvcmQ=',
                isLoggedIn: true,
                roles: ['testRole1', 'testRole2', 'testRole3']
            };

            // -- act -- //
            loginService.setCredentials(username, password, roles);
            var user = stewardService.getAppUser();

            // -- assert --/
            expect(user).toEqual(expectedResult);
        });

        it('clearCredentials should clear the user', function () {
            // -- arrange -- //
            var expectedResult = {};

            // -- act -- //
            loginService.clearCredentials();
            var user = stewardService.getAppUser();

            // -- assert --/
            expect(user).toEqual({});
        });

    }
})();
