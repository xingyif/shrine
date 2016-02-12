(function () {
    'use strict';

    // -- register with angular -- //
    angular.module('shrine-tools')
        .controller('QEPController', QEPController);


    /**
     *
     * @type {string[]}
     */
    QEPController.$inject = ['$app'];
    function QEPController ($app) {
        var vm = this;
    }
})();
