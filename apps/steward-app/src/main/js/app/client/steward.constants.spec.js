(function () {
    'use strict';

    describe('shrine.steward constants tests', StewardConstantsSpec);

    function StewardConstantsSpec() {

        // -- vars -- //
        var stewardConstants;

        function setup() {
            module('shrine.steward');
            inject(function (constants) {
                stewardConstants = constants;
            });
        }

        //-- setup --/
        beforeEach(setup);

        // -- tests -- //
        it('constants should exist.', function () {
            expect(stewardConstants).toBeDefined();
        });

        // -- tests -- //
        it('defaultRoute member should exist.', function () {
            expect(stewardConstants.defaultRoute).toBeDefined();
        });

        // -- tests -- //
        it('baseUrl member should exist.', function () {
            expect(stewardConstants.baseUrl).toBeDefined();
        });

        // -- tests -- //
        it('restOptions member should exist.', function () {
            expect(stewardConstants.restOptions).toBeDefined();
        });

        // -- tests -- //
        it('restInterpolators member should exist.', function () {
            expect(stewardConstants.restInterpolators).toBeDefined();
        });

        // -- tests -- //
        it('states member should exist.', function () {
            expect(stewardConstants.states).toBeDefined();
        });

        // -- tests -- //
        it('roles member should exist.', function () {
            expect(stewardConstants.roles).toBeDefined();
        });

        // -- tests -- //
        it('testPort member should exist.', function () {
            expect(stewardConstants.testPort).toBeDefined();
        });

        // -- tests -- //
        it('title member should exist.', function () {
            expect(stewardConstants.title).toBeDefined();
        });
    }
})();
