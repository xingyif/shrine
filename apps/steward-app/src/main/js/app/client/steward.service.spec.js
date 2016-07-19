(function () {
    'use strict';

    describe('shrine.steward StewardService tests', StewardServiceSpec);

    function StewardServiceSpec() {

        // -- vars -- //
        var stewardService

        function setup() {
            module('shrine.steward');
            inject(function (StewardService) {
                stewardService = StewardService;
            });
        }

        //-- setup --/
        beforeEach(setup);


        function loginUser() {
            var username = 'ben';
            var password = 'kapow';
            var authData = stewardService.commonService.toBase64(username + ':' + password);
            var roles = ['admin','steward','researcher'];
            var appUser = {
                username: 'ben',
                authData: authData,
                isLoggedIn:true,
                roles: roles
            };

            stewardService.setAppUser(username, authData, roles);
        }

        
        it('commonService member should exist.', function () {
            expect(stewardService.commonService).toBeDefined();
        });

        it('constants member should exist.', function () {
            expect(stewardService.constants).toBeDefined();
        });

        it('setAppUser and getAppUser user should work.', function () {
            loginUser();

            var user = stewardService.getAppUser();

            expect(user).toBeDefined();

        });

        it('deleteAppUser and isUserLoggedIn user should work.', function () {
            loginUser();

            stewardService.deleteAppUser();

            var isUserLoggedIn = stewardService.isUserLoggedIn();

            expect(isUserLoggedIn).toBe(false);

        });


        it('isUserLoggedIn should work.', function () {
            loginUser();

            var isUserLoggedIn = stewardService.isUserLoggedIn();

            expect(isUserLoggedIn).toBe(true);

        });

        it('getUsername should work.', function () {
            loginUser();

            var user = stewardService.getAppUser();
            var username = stewardService.getUsername();

            expect(username).toBe(user.username);

        });

        it('getRole should work.', function () {
            loginUser();
            var user = stewardService.getAppUser();
            var role = stewardService.getRole();
            expect(role).toBe(user.roles[0]);
        });
    }
})();
