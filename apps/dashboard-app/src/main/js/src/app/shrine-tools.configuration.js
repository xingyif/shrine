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
                    'src/app/common/authentication/authentication-model.js',
                    'src/app/common/authentication/authentication.js',
                    'src/app/login/LoginCtrl.js'
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
                    'src/app/diagnostic/views/happy/happy-model.js',
                    'src/app/diagnostic/views/happy/happy.js'
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
                url:'/login',
                controller: 'LoginCtrl',
                templateUrl:'src/app/login/login.tpl.html',
                resolve: {
                    loadFiles:function($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['login']);
                    }
                }
            })
            .state('diagnostic', {
                url:'/diagnostic',
                controller: 'STCtrl',
                templateUrl: 'src/app/diagnostic/diagnostic.tpl.html',
                resolve: {
                    loadFiles:function($ocLazyLoad){
                        return $ocLazyLoad.load(stateConfig['diagnostic'])
                    }
                }
            })
            .state('diagnostic.summary',{
                url:'/summary',
                templateUrl:'src/app/diagnostic/views/summary.tpl.html',
                resolve: {
                    loadMyFiles:function($ocLazyLoad) {
                        return $ocLazyLoad.load(stateConfig['diagnostic.summary']);
                    }
                }
            })
            .state('diagnostic.i2b2-connections',{
                url:'/i2b2-connections',
                templateUrl:'src/app/diagnostic/views/i2b2-connections.tpl.html'
            })
            .state('diagnostic.keystore',{
                url:'/keystore',
                templateUrl:'src/app/diagnostic/views/keystore.tpl.html'
            })
            .state('diagnostic.hub',{
                url:'/hub',
                templateUrl:'src/app/diagnostic/views/hub.tpl.html'
            })
            .state('diagnostic.adapter',{
                url:'/adapter',
                templateUrl:'src/app/diagnostic/views/adapter.tpl.html'
            })
            .state('diagnostic.qep',{
                url:'/qep',
                templateUrl:'src/app/diagnostic/views/qep.tpl.html'
            })
            .state('diagnostic.config',{
                url:'/config',
                templateUrl:'src/app/diagnostic/views/config.tpl.html'
            })
            .state('diagnostic.downstream-nodes',{
                url:'/downstream-nodes'
            });
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




