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

        it('Steward Service should exist', function () {
            expect(stewardController.stewardService).toBeDefined();
        })
    }
})();
