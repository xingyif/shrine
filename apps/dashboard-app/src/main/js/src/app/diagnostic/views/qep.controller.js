(function () {
    'use strict';

    // -- register with angular -- //
    angular.module('shrine-tools')
        .controller('QEPController', QEPController);


    /**
     *
     * @type {string[]}
     */
    QEPController.$inject = ['$app', '$log'];
    function QEPController ($app, $log) {
        var vm = this;
        vm.optionsError = false;
        vm.qepError = false;
        init();

        function init () {

            $app.model.getOptionalParts()
                .then(setOptions, handleOptionsFailure)
                .then($app.model.getQep)
                .then(setQep, handleQepFailure);
        }

        function handleOptionsFailure(failure) {
            //TODO: HANDLE FAILURE
            vm.optionsError = failure;
        }

        function handleQepFailure(failure) {
            //TODO: HANDLE FAILURE
            vm.qepError = failure;
        }

        function setOptions(options) {
            vm.isStewardEnabled = options.stewardEnabled;
            vm.isDownstream = !options.isHub;

        }

        function setQep(qep) {
            vm.steward = qep.steward;
            vm.broadcasterUrl = (vm.isDownstream && qep.broadcasterUrl !== undefined)?
                                    qep.broadcasterUrl:
                                    'Unknown';
            vm.trustModel = qep.trustModel;
        }

    }
})();
