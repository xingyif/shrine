(function () {
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
            'model-service'
        ]);
})();
