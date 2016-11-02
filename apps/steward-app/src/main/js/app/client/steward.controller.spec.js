(function () {
    'use strict';

    describe('shrine.steward controller tests', CommonServiceSpec);

    function CommonServiceSpec() {

        // -- vars -- //
        var stewardController;

        function setup() {
            module('shrine.steward');
            inject(function ($controller) {
                stewardController = $controller('StewardController', {});
            });
        }

        //-- setup --/
        beforeEach(setup);

        it('StewardController should exist', function () {
            expect(typeof (stewardController)).toBe('object');
        });

        it('isUserLoggedIn should exist', function () {
            expect(stewardController.isUserLoggedIn).toBeDefined();
        });

        it('getUsername should exist', function () {
            expect(stewardController.getUsername).toBeDefined();
        });

        xit('getRole should exist', function () {
            expect(stewardController.getRole).toBeDefined();
        });
    }
})();
