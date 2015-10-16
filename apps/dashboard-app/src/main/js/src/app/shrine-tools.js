'use strict';
/**
 * @ngdoc overview
 * @name sbAdminApp
 * @description
 * # sbAdminApp
 *
 * Main module of the application.
 */
angular
    .module('shrine-tools', [
        'oc.lazyLoad',
        'ui.router',
        'ui.bootstrap',
        'ui.bootstrap.modal',
        'angular-loading-bar',
        'app-utils',
        'model-service',
        'st-model'
    ])
    .config(['$stateProvider', '$urlRouterProvider', '$ocLazyLoadProvider', '$httpProvider',  function ($stateProvider, $urlRouterProvider, $ocLazyLoadProvider, $httpProvider) {

        $ocLazyLoadProvider.config({
            debug: false,
            events:true
        });

        $urlRouterProvider.otherwise('/dashboard/diagnostics');
        $httpProvider.defaults.useXDomain = true;
        delete $httpProvider.defaults.headers.common['X-Requested-With'];

        // -- If running from deployment, No IE Cache -- //
        if (window.location.origin.indexOf('http://localhost:63342') === -1) {

            //http://stackoverflow.com/questions/16098430/angular-ie-caching-issue-for-http
            //initialize get if not there
            if (!$httpProvider.defaults.headers.get) {
                $httpProvider.defaults.headers.get = {};
            }
            //disable IE ajax request caching
            $httpProvider.defaults.headers.get['If-Modified-Since'] = 'Sat, 26 Jul 1997 05:00:00 GMT';
            // extra
            $httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache';
            $httpProvider.defaults.headers.get['Pragma'] = 'no-cache';
        }

        $stateProvider
            .state('login',{
                url:'/login',
                controller: 'LoginCtrl',
                templateUrl:'src/app/login/login.tpl.html',
                resolve: {
                    loadMyFiles:function($ocLazyLoad) {
                        return $ocLazyLoad.load({
                            name:'login',
                            files:[
                                'src/app/common/authentication/authentication-model.js',
                                'src/app/common/authentication/authentication.js',
                                'src/app/login/LoginCtrl.js'
                            ]
                        });
                    }
                }
            })
            .state('dashboard', {
                url:'/dashboard',
                controller: 'STCtrl',
                templateUrl: 'src/app/dashboard/main.tpl.html',
                resolve: {
                    loadMyDirectives:function($ocLazyLoad){
                        return $ocLazyLoad.load(
                            {
                                name:'shrine-tools',
                                files:[
                                    'src/app/dashboard/MainCtrl.js',
                                    'src/app/dashboard/sidebar/sidebar.js'
                                ]
                            }),
                            $ocLazyLoad.load(
                                {
                                    name:'toggle-switch',
                                    files:["src/vendor/angular-toggle-switch/angular-toggle-switch.min.js",
                                        "src/vendor/angular-toggle-switch/angular-toggle-switch.css"
                                    ]
                                }),
                            $ocLazyLoad.load(
                                {
                                    name:'ngAnimate',
                                    files:['src/vendor/angular-animate/angular-animate.js']
                                })
                        $ocLazyLoad.load(
                            {
                                name:'ngCookies',
                                files:['src/vendor/angular-cookies/angular-cookies.js']
                            })
                        $ocLazyLoad.load(
                            {
                                name:'ngResource',
                                files:['src/vendor//angular-resource/angular-resource.js']
                            })
                        $ocLazyLoad.load(
                            {
                                name:'ngSanitize',
                                files:['src/vendor/angular-sanitize/angular-sanitize.js']
                            })
                        $ocLazyLoad.load(
                            {
                                name:'ngTouch',
                                files:['src/vendor/angular-touch/angular-touch.js']
                            });
                    }
                }
            })
            .state('dashboard.happy',{
                url:'/diagnostics',
                controller: 'HappyCtrl',
                templateUrl:'src/app/dashboard/apps/happy/happy.tpl.html',
                resolve: {
                    loadMyFiles:function($ocLazyLoad) {
                        return $ocLazyLoad.load({
                            name:'shrine-happy',
                            files:[
                                'src/app/dashboard/apps/happy/happy-model.js',
                                'src/app/dashboard/apps/happy/happy.js'
                            ]
                        });
                    }
                }
            });
    }])
/**
 * App Run Phase - Set up listener to verify user has access.
 */
    .run(function ($rootScope, $location, AppUtilsService, $app) {

        $rootScope.$on( "$locationChangeStart", function (event, next, current) {
            // redirect to login page if not logged in
            if ($location.path() !== '/login' && (!$app.globals.currentUser || !$app.globals.currentUser.isLoggedIn)) {
                $location.path('/login');
            }
        });
    })
    .service("$app", function ($rootScope, AppUtilsService) {
        //set app globals
        $rootScope.app = {
            globals: {
                AppTitleBase: "SHRINE TOOLS",
                UserRoles:  {
                    ROLE1: "Researcher",
                    ROLE2: "DataSteward",
                    ROLE3: "Admin"
                },
                ViewConfig: {
                    INDEX: 1,
                    RANGE: 4,
                    LIMIT: 5
                },
                States:     {
                    STATE1: "Pending",
                    STATE2: "Approved",
                    STATE3: "Rejected"
                }
            },
            utils:   AppUtilsService
        };

        // -- aggregate methods that access $rootscope. -- //
        $rootScope.app.utils.setAppUser = function (username, authdata, roles) {

            var userRoles = [roles[roles.length -1]];
            $rootScope.app.globals.currentUser = {
                username:   username,
                authdata:   authdata,
                isLoggedIn: true,
                roles:      userRoles
            };
        };

        $rootScope.app.utils.setAppTitle = function () {
            var titleArray  = $rootScope.app.globals.AppTitleBase.split(' ');
            titleArray[2]   = $rootScope.app.globals.currentUser.roles[0].toUpperCase();
            $rootScope.app.globals.AppTitle = titleArray.join(' ');
        };

        $rootScope.app.utils.clearAppTitle = function () {
            $rootScope.app.globals.AppTitle = $rootScope.app.globals.AppTitleBase;
        };

        $rootScope.app.utils.deleteAppUser = function () {
            delete $rootScope.app.globals.currentUser;
        };

        return {
            globals: $rootScope.app.globals,
            utils:   $rootScope.app.utils
        };

    })
    .controller('STCtrl', ['$rootScope', '$scope', '$location', '$app', 'STMdl', function ($rootScope, $scope, $location, $app, STMdl) {
        $scope.bannerUrl = '';
        $scope.helpUrl   = '';

        $scope.$app = $app;

        $scope.getConfigData = function () {
            STMdl.getConfig()
                .then(function (data) {
                    $scope.bannerUrl = data.banner;
                    $scope.footerUrl = data.footer;
                    $scope.helpUrl   = data.help;
                });
        };

        $scope.getUsername = function () {
            return ($app.globals.currentUser) ?
                $app.globals.currentUser.username : '';
        };

        $scope.getRole = function () {
            if (!$app.globals.currentUser) {
                return '';
            }
            var idx = $app.globals.currentUser.roles.length - 1;
            return $app.globals.currentUser.roles[idx];
        };

        $scope.isUserLoggedIn = function () {
            return ($app.globals.currentUser !== undefined && $app.globals.currentUser.isLoggedIn === true);
        };

        $scope.logoImgFallback = function() {
            // sorted from highest to lowest priority
            var potentialUrls = ["/static/logo.png","/static/logo.jpg","/static/logo.gif","src/assets/img/logo_placeholder.png"];

            var src = "src/assets/img/logo_placeholder.png";
            var i;

            for(i = 0; i < potentialUrls.length; i++) {
                $http.get(url).then(function(response) { // success
                    src = url;
                    break;
                }, function(response) { // error
                    continue;
                }); 
            };
            
            return src;
        };

        $scope.logout = function () {
            $location.path('/login');
        };

        $scope.getConfigData();
    }]);



