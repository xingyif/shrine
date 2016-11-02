(function () {
    'use strict';

    describe('shrine.common CommonService tests', CommonServiceSpec);

    function CommonServiceSpec() {

        // -- vars -- //
        var commonService;
        var user = {
            roles: ['admin']
        };

        function setup() {
            module('shrine.common');
            inject(function (CommonService) {
                commonService = CommonService;
            });
        }

        //-- setup --/
        beforeEach(setup);

        
        it('CommonService should exist', function () {
            expect(typeof (commonService)).toBe('object');
        });

        it('CommonService.dateService should be defined', function () {
            expect(typeof (commonService.dateService)).toBe('object');
        });

        it('hasAccess should return true if user is in list of roles', function () {

            // -- arrange -- //
            var roles = ['admin', 'role1', 'role2'];

            // -- act --//
            var result = commonService.hasAccess(user, roles);

            //-- assert --//
            expect(result).toBe(true);
        });

        it('toBase64 should return a formatted base64 encrypted string if given a password', function () {

            // -- arrange -- //
            var password = 'password';
            var expectedResult = 'cGFzc3dvcmQ=';

            // -- act --//
            var result = commonService.toBase64(password);

            //-- assert --//
            expect(result).toBe(expectedResult);
        });
    }
})();
