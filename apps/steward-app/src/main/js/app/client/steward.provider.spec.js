(function () {
    'use strict';

    describe('shrine.steward StewardProvider tests', StewardProviderSpec);

    function StewardProviderSpec() {


        // -- vars -- //
        var stewardProvider

        //http://stackoverflow.com/questions/14771810/how-to-test-angularjs-custom-provider
        function setup() {
            /**
             * Create a mock module and inject the provider
             * in order to initialize the stewardProvider.
             *  */ 
            angular.module('shrine.steward.mock',[])
            .config(function(StewardServiceProvider) {
                stewardProvider = StewardServiceProvider;
            });

            module('shrine.steward', 'shrine.steward.mock');
            inject(function () {
            });
        }

        //-- setup --/
        beforeEach(setup);

        // -- tests -- //
        it('StewardProvider should exist.', function () {
            expect(stewardProvider).toBeDefined();
        });

        it('$get should yield an instance of StewardService', function () {
            var shrineService = stewardProvider.$get();
            expect(shrineService.setAppUser).toBeDefined();
        });

        it('configureHttpProvider should be defined', function () {
            expect(stewardProvider.configureHttpProvider).toBeDefined();
        });

        it('constants should be defined', function () {
            expect(stewardProvider.constants).toBeDefined();
        });
    }
})();
