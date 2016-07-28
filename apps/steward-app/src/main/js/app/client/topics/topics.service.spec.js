(function () {
    'use strict';

    describe('shrine.steward.topics TopicsService tests', TopicsServiceSpec);

    function TopicsServiceSpec() {

        // -- vars -- //
        var topicsService;
        var stewardService;

        function setup() {
            module('shrine.steward.topics');
            inject(function (TopicsService, StewardService) {
                topicsService = TopicsService;
                stewardService = StewardService;
            });
        }

        function loginUser() {
            var username = 'ben';
            var password = 'kapow';
            var authData = stewardService.commonService.toBase64(username + ':' + password);
            var role = 'DataSteward';
            var appUser = {
                username: 'ben',
                authData: authData,
                isLoggedIn:true,
                roles: role
            };

            stewardService.setAppUser(username, authData, ['Role1', 'Role2']);
        }


        //-- setup --/
        beforeEach(setup);


        it('Service  should exist.', function () {
            expect(topicsService).toBeDefined();
        });

        it('isSteward should be work', function() {
            loginUser();
            var result =  topicsService.isSteward();
            expect(result).toBe(true);
        });
    }
})();
