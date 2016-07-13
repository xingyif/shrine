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

        // -- tests -- //
        it('commonService member should exist.', function () {
            expect(stewardService.commonService).toBeDefined();
        });

        // -- tests -- //
        it('constants member should exist.', function () {
            expect(stewardService.constants).toBeDefined();
        });

        // -- tests -- //
        it('setAppUser and getAppUser user should work.', function () {
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

            var user = stewardService.getAppUser();

            expect(user).toBeDefined();

        });
    }
})();
