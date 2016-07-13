(function () {
    'use strict';

    describe('shrine.steward StewardProvider tests', StewardProviderSpec);

    function StewardProviderSpec() {

        // -- vars -- //
        var stewardProvider

        //http://stackoverflow.com/questions/14771810/how-to-test-angularjs-custom-provider
        function setup() {
            module('shrine.steward');
            inject(function (StewardService) {
                stewardProvider = StewardService;
            });
        }

        //-- setup --/
        beforeEach(setup);

        // -- tests -- //
        it('commonService member should exist.', function () {
            expect(undefined).toBeDefined();
        });
    }
})();
