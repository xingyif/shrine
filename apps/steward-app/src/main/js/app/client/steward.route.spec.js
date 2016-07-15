(function () {
    'use strict';

    describe('shrine.steward.route tests', StewardRouteSpec);

    function StewardRouteSpec() {

        // -- vars -- //
        var $state;
        var $rootScope;
        var templateCache;
        var stewardService;

        var state = 'topics';
        var user = {
            username: 'testUser',
            password: 'kapow',
            roles: ['testRole1','testRole2','testRole3']
        };

        function setup() {
            module('shrine.steward');
            inject(function (_$state_, $templateCache, _$rootScope_, _StewardService_) {
                $state = _$state_
                $rootScope = _$rootScope_;
                templateCache = $templateCache;
                stewardService = _StewardService_;
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
            stewardService.setAppUser(user.username, user.password, user.roles);
            activateRoute('app/client/topics/topics.tpl.html', 'topics');
            expect($state.href($state.current.name)).toEqual('#/topics');
        });

                // -- tests -- //
        it('login route url should be set to #/login', function () {
            stewardService.deleteAppUser();
            activateRoute('app/client/login/login.tpl.html', 'login');
            expect($state.href($state.current.name)).toEqual('#/login');
        });
    }
}
)();
