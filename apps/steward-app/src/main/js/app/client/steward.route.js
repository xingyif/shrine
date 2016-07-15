(function () {
    'use strict';  
     var homeRoute = '/topics';
    
    angular
        .module('shrine.steward')
        .config(StewardConfiguration)

    StewardConfiguration.$inject = ['$stateProvider', '$urlRouterProvider', '$httpProvider', 'StewardServiceProvider'];
    function StewardConfiguration($stateProvider, $urlRouterProvider, $httpProvider, stewardProvider) {

        // -- default route --//
        $urlRouterProvider.otherwise(homeRoute);

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
                templateUrl:'app/client/login/login.tpl.html',
                controllerAs:'login'
            });

    }
})();
