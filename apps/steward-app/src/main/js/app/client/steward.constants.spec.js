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

        
        it('constants should exist.', function () {
            expect(stewardConstants).toBeDefined();
        });

        
        it('homeRoute member should exist.', function () {
            expect(stewardConstants.homeRoute).toBeDefined();
        });

        
        it('baseUrl member should exist.', function () {
            expect(stewardConstants.baseUrl).toBeDefined();
        });

        
        it('restOptions member should exist.', function () {
            expect(stewardConstants.restOptions).toBeDefined();
        });

        
        it('restInterpolators member should exist.', function () {
            expect(stewardConstants.restInterpolators).toBeDefined();
        });

        
        it('states member should exist.', function () {
            expect(stewardConstants.states).toBeDefined();
        });

        
        it('roles member should exist.', function () {
            expect(stewardConstants.roles).toBeDefined();
        });

        
        it('title member should exist.', function () {
            expect(stewardConstants.title).toBeDefined();
        });
    }
})();
