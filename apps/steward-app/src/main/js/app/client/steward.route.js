(function () {
    'use strict';  
     var defaultRoute = '/topics';
    
    angular
        .module('shrine.steward')
        .config(StewardConfiguration)
        .run(StewardRunner);

    StewardConfiguration.$inject = ['$stateProvider', '$urlRouterProvider', '$httpProvider', 'StewardServiceProvider'];
    function StewardConfiguration($stateProvider, $urlRouterProvider, $httpProvider, stewardProvider) {

        // -- default route --//
        $urlRouterProvider.otherwise(defaultRoute);

        stewardProvider.configureHttpProvider($httpProvider);

        // -- configure states -- //
        $stateProvider
            .state('topics',{
                url:'/topics',
                controller: 'TopicsController',
                templateUrl:'app/client/topics/topics.tpl.html',
                controllerAs:'topics'
            })
            .state('login',{
                url:'/login',
                controller: 'LoginController',
                templateUrl:'app/client/login/topics.tpl.html',
                controllerAs:'login'
            });

    }

    /**
    * App Run Phase - Set up listener to verify user has access.
    */
    StewardRunner.$inject = ['$rootScope', '$location', 'StewardService'];
    function StewardRunner($rootScope, $location, StewardService) {
        $rootScope.$on('$locationChangeStart', authenticateUser);

        function authenticateUser(event, next, current) {
            // redirect to login page if not logged in
            // // redirect to login page if not logged in
            // if ($location.path() !== '/login' && (!$app.globals.currentUser || !$app.globals.currentUser.isLoggedIn)) {
            //     $location.path('/login');
            // }
        }
    }
})();
