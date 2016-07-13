(function () {
    'use strict';

    describe('shrine.steward.route tests', StewardRouteSpec);

    function StewardRouteSpec() {

        // -- vars -- //
        var $state;
        var $rootScope;
        var templateCache;
        var state = 'topics';

        function setup() {
            module('shrine.steward');
            inject(function (_$state_, $templateCache, _$rootScope_) {
                $state = _$state_
                $rootScope = _$rootScope_;
                templateCache = $templateCache;
            });
        }

        function activateRoute(template, name) {
            templateCache.put(template, '');
            $state.go(name);
            $rootScope.$digest();
        }

        //-- setup --/
        beforeEach(setup);

        // -- tests -- //
        it('topics route url should be set to #/topics', function () {
            activateRoute('app/client/topics/topics.tpl.html', 'topics');
            expect($state.href(state)).toEqual('#/topics');
        });

        // Test whether our state activates correctly
        it('topics route should activate the state', function () {
            activateRoute('app/client/topics/topics.tpl.html', 'topics');
            expect($state.current.name).toBe('topics');
        });
    }
}
)();
