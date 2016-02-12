(function () {
    'use strict';


    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('HubController', HubController);


    /**
     *
     * @type {string[]}
     */
    HubController.$inject = ['$app'];
    function HubController ($app) {
        var vm = this;
    }
})();
