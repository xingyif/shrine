describe("MainCtrl", function () {

    var $location, rootScope, scope, app, appMdl, controller;

    beforeEach(module('stewardApp'));

    beforeEach(inject(function ($rootScope, $controller , _$location_, _$app_, _AppMdl_) {
        $location   = _$location_;
        scope       = $rootScope.$new();
        rootScope   = $rootScope;
        app         = _$app_,
        appMdl      = _AppMdl_,
        controller  = $controller;


        function createController () {
             return $controller('MainCtrl', {
                    '$rootScope':  rootScope,
                    '$scope':       scope,
                    '$location':  $location,
                    '$app': app,
                    'AppMdl': appMdl
             });
         };
    }));

    describe("app-model.js", function () {

        it('App Model should exist', function () {
            expect('appMdl').toBeTruthy();
        });

    });

    describe('MainCtrl.js', function () {
        it('MainCtrl scope should be initialized', function () {
            var MainCtrl = controller('MainCtrl', {
                $rootScope:  rootScope,
                $scope:       scope,
                $location:  $location,
                $app: app,
                AppMdl: appMdl
            });


            expect(typeof(scope.getConfigData)).toBe('function');
        });
    });



});

