(function () {
    'use strict';
    angular
        .module('shrine-tools')
        .config(ShrineToolsConfiguration);

    // --  todo: move to service or constant  -- //
    var stateConfig     = {
            'login': {
                name:'login',
                files:[
                    'src/app/common/authentication/authentication.module.js',
                    'src/app/common/authentication/authentication.service.js',
                    'src/app/login/login.controller.js'
                ]
            },

            'diagnostic': {
                name:'diagnostic',
                files:[
                    'src/app/diagnostic/sidebar/sidebar.model.js',
                    'src/app/diagnostic/sidebar/sidebar.service.js',
                    'src/app/diagnostic/sidebar/node-menu.js',
                    'src/app/diagnostic/sidebar/downstream-node.js',
                    'src/app/diagnostic/sidebar/sidebar.js'
                ]
            },
            'diagnostic.summary': {
                name:'diagnostic.summary',
                files:[
                    'src/app/diagnostic/views/summary.controller.js'
                ]
            },
            'diagnostic.i2b2-connections': {
                name:'diagnostic.i2b2-connections',
                files:[
                    'src/app/diagnostic/views/i2b2-connections.controller.js'
                ]
            },
            'diagnostic.keystore': {
                name:'diagnostic.keystore',
                files:[
                    'src/app/diagnostic/views/keystore.controller.js'
                ]
            },
            'diagnostic.hub': {
                name:'diagnostic.keystore',
                files:[
                    'src/app/diagnostic/views/hub.controller.js'
                ]
            },
            'diagnostic.adapter': {
                name:'diagnostic.adapter',
                files:[
                    'src/app/diagnostic/views/adapter.controller.js'
                ]
            },
            'diagnostic.qep': {
                name:'diagnostic.qep',
                files:[
                    'src/app/diagnostic/views/qep.controller.js'
                ]
            },
            'diagnostic.config': {
                name: 'diagnostic.config',
                files: [
                    'src/app/diagnostic/views/config.controller.js',
                    'src/app/diagnostic/views/bootcordion.js'
                ]
            },
            'diagnostic.problems': {
                name: 'diagnostic.problems',
                files: [
                    'src/app/diagnostic/views/problems.controller.js'
                ]
            }
        };


    ShrineToolsConfiguration.$inject = ['$stateProvider', '$urlRouterProvider', '$ocLazyLoadProvider', '$httpProvider'];
    function ShrineToolsConfiguration ($stateProvider, $urlRouterProvider, $ocLazyLoadProvider, $httpProvider) {

        // -- set default view -- //
        $urlRouterProvider.otherwise('/diagnostic/summary');

        configureLazyLoader($ocLazyLoadProvider);
        configureHttpProvider($httpProvider);

        $stateProvider
            .state('login',{
                url:          '/login',
                controller:   'LoginController',
                controllerAs: 'vm',
                templateUrl:  'src/app/login/login.tpl.html',
                resolve: {
                    loadFiles:function($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['login']);
                    }
                }
            })
            .state('diagnostic', {
                url:          '/diagnostic',
                controller:   'STCtrl',
                templateUrl:  'src/app/diagnostic/diagnostic.tpl.html',
                resolve: {
                    loadFiles:function($ocLazyLoad){
                        return $ocLazyLoad.load(stateConfig['diagnostic'])
                    }
                }
            })
            .state('diagnostic.summary',{
                url:          '/summary',
                controller:   'SummaryController',
                controllerAs: 'vm',
                templateUrl:  'src/app/diagnostic/views/summary.tpl.html',
                resolve: {
                    loadMyFiles:function($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.summary']);
                    }
                }
            })
            .state('diagnostic.i2b2-connections',{
                url:          '/i2b2-connections',
                controller:   'I2B2ConnectionsController',
                controllerAs: 'vm',
                templateUrl:  'src/app/diagnostic/views/i2b2-connections.tpl.html',
                resolve: {
                    loadMyFiles:function($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.i2b2-connections']);
                    }
                }
            })
            .state('diagnostic.keystore',{
                url:          '/keystore',
                templateUrl:  'src/app/diagnostic/views/keystore.tpl.html',
                controller:   'KeystoreController',
                controllerAs: 'vm',
                resolve: {
                    loadMyFiles: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.keystore']);
                    }
                }
                //@todo: load files
            })
            .state('diagnostic.hub',{
                url:'/hub',
                templateUrl:  'src/app/diagnostic/views/hub.tpl.html',
                controller: 'HubController',
                controllerAs: 'vm',
                resolve: {
                    loadMyFiles: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.hub'])
                    }
                }
                //@todo: load files
            })
            .state('diagnostic.adapter',{
                url:          '/adapter',
                templateUrl:  'src/app/diagnostic/views/adapter.tpl.html',
                controller:   'AdapterController',
                controllerAs: 'vm',
                resolve: {
                    loadMyFiles:function($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.adapter']);
                    }
                }
            })
            .state('diagnostic.qep',{
                url:          '/qep',
                templateUrl:    'src/app/diagnostic/views/qep.tpl.html',
                controller:     'QEPController',
                controllerAs:   'vm',
                resolve: {
                    loadMyFiles: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.qep']);
                    }
                }
            })
            .state('diagnostic.config',{
                url:           '/config',
                controller:    'ShrineConfigurationController',
                controllerAs:  'vm',
                templateUrl:   'src/app/diagnostic/views/config.tpl.html',
                resolve: {
                    loadFiles: function ($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.config']);
                    }
                }
            })
            .state('diagnostic.downstream-nodes',{
                url:'/downstream-nodes'
                //@todo: load files
            })
            .state('diagnostic.problems', {
                url:            '/problems',
                controller:     'ProblemsController',
                controllerAs:   'vm',
                templateUrl:    'src/app/diagnostic/views/problems.tpl.html',
                resolve: {
                    loadFiles: function($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.problems']);
                    }
                }
            })
    }

    /**
     * Configure lazy loader to log all errors to console, broadcast event when file,
     * module or component loads
     * @param lazyLoader
     * @todo: configure module dependencies here.
     * @see: https://oclazyload.readme.io/docs/oclazyloadprovider
     */
    function configureLazyLoader(lazyLoader) {

        var lazyLoadConfig = {
            debug: true,
            events:true
        }

        lazyLoader.config(lazyLoadConfig);

        return lazyLoader;
    }

    /**
     * Set up cross domain voodoo, if running from deployment, No IE Cache
     * @param httpProvider
     * @returns {*}
     * @see: http://stackoverflow.com/questions/16098430/angular-ie-caching-issue-for-http
     */
    function configureHttpProvider (httpProvider) {

        // -- set up cross domain -- //
        httpProvider.defaults.useXDomain = true;
        delete httpProvider.defaults.headers.common['X-Requested-With'];

        // -- If running from deployment, No IE Cache -- //
        if (window.location.origin.indexOf('http://localhost:63342') === -1) {

            //initialize get if not there
            if (!httpProvider.defaults.headers.get) {
                httpProvider.defaults.headers.get = {};
            }
            //disable IE ajax request caching
            httpProvider.defaults.headers.get['If-Modified-Since']     = 'Sat, 26 Jul 1997 05:00:00 GMT';
            httpProvider.defaults.headers.get['Cache-Control']         = 'no-cache';
            httpProvider.defaults.headers.get['Pragma']                = 'no-cache';
        }

        return httpProvider;
    }

})();




