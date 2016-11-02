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
            var role = 'DataSteward';
            var appUser = {
                username: 'ben',
                authData: authData,
                isLoggedIn:true,
                roles: role
            };

            stewardService.setAppUser(username, authData, ['Role1', 'Role2']);
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
            var roleResult = stewardService.getRole();
            expect(roleResult).toBe('DataSteward');
        });

//"https://shrine-dev1.catalyst:6443/steward/steward/topics?skip=0&limit=20&state=Pending&sortBy=createDate&sortDirection=ascending"
//"https://shrine-dev1.catalyst:6443/steward/researcher/topics?skip=0&limit=20&sortBy=changeDate&sortDirection=ascending"        
//'http://localhost:6443/steward/researcher/topics?skip=0&limit=15&state=Pending&sortBy=createDate&sortDirection=ascending&minDate=12345&maxDate=12345'
        it('getQueryString should work', function() {
            var expectedResult = 'http://localhost:6443/steward/researcher/topics?skip=0&limit=15&state=Pending&sortBy=createDate&sortDirection=ascending&minDate=12345&maxDate=12345'
            var result = stewardService.getUrl('researcher/topics', 0,15,'Pending', 'createDate', 'ascending', 12345, 12345)

            expect(result).toBe(expectedResult);
        });
        //http://localhost:6443/steward/researcher/topics?skip=0&limit=15&state=Pending&sortBy=createDate&sortDirection=ascending&minDate=12345&maxDate=12345
        //http://localhost:6443/steward/researcher/topics?limit=15&state=Pending&sortBy=createDate&sortDirection=ascending&minDate=12345&maxDate=12345'
    }
})();
