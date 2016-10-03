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

        init();

        function init () {

            $app.model.getOptions()
                .then(setOptions, handleFailure)
                .then($app.model.getQep, handleFailure)
                .then(setQep, handleFailure);
        }

        function handleFailure(failure) {
            //TODO: HANDLE FAILURE
            $log.error(JSON.stringify(failure));
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
        }

    }
})();
