(function () {
    'use strict';


    describe('loginModel services', LoginModelSpec);

    function LoginModelSpec() {

        var loginModel, $httpBackend, constants;

        function setup() {
            module('shrine.steward.login');

            inject(function (_$httpBackend_, _LoginModel_, _constants_) {
                $httpBackend = _$httpBackend_;
                loginModel = _LoginModel_;
                constants = _constants_;
                $httpBackend.whenGET(/\.html$/).respond('');
            });
        }

        beforeEach(setup);

        it('loginModel.bar() - test', function () {
            var mockData = {
                statusText: 'success',
                userId: 'test',
                roles: ['testRole1,testRole2,testRole3']
            };

            var expectedResult = {
                success: true,
                msg: 'success',
                userId: mockData.userId,
                roles: mockData.roles
            }

            $httpBackend.expectGET(constants.baseUrl + 'user/whoami/').respond(mockData);
            loginModel.login().then(function (data) {
                expect(data.userId).toEqual(mockData.userId);
            });
            $httpBackend.flush();
        });
    }
})();
