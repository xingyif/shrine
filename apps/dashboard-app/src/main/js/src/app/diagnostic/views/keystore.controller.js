(function () {
    'use strict'

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('KeystoreController', KeystoreController);


    /**
     *
     * @type {string[]}
     */
    KeystoreController.$inject = ['$app', '$log'];
    function KeystoreController ($app, $log) {
        var vm = this;

        init();


        /**
         *
         */
        function init() {
            $app.model.getKeystore()
                .then(setKeystore, handleFailure);
        }


        /**
         *
         * @param all
         */
        function setKeystore (keystore) {
            vm.keystore = {
                file:       keystore.fileName,
                password:   "REDACTED"
            };
            vm.certificate = {
                alias:           keystore.privateKeyAlias,
                owner:           keystore.owner, //todo config.keystore.certId.name,
                issuer:          keystore.issuer, //todo: verify the source,
                privateKeyAlias: keystore.caTrustedAlias //todo: Why are these the same?

            }
        }

        //TODO: Good error handling
        function handleFailure(failure) {
            $log.error(JSON.stringify(failure));
        }
    }
})();