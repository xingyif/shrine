(function () {
    'use strict'

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('KeystoreController', KeystoreController);


    /**
     *
     * @type {string[]}
     */
    KeystoreController.$inject = ['$app']
    function KeystoreController ($app) {
        var vm = this;
    }

})();