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

            $app.model.getQep()
                .then(setQep, handleFailure)
        }

        function setQep(qep) {
            vm.trustModelIsHub = qep.trustModelIsHub
        }

        /**
         *
         * @param keystore
         */
        function setKeystore (keystore) {
            vm.keystore = {
                file:       keystore.fileName,
                password:   "REDACTED"
            };
            vm.certificate = [
                ['Alias',             keystore.privateKeyAlias],
                ['Owner',             keystore.owner],
                ['Issuer',            keystore.issuer],
                ['Expires',           keystore.expires],
                ['Private Key Alias', keystore.caTrustedAlias],
                ['MD5 Signature',     keystore.md5Signature],
                ['SHA256 Signature',  keystore.sha256Signature]
            ];

            vm.caCertificate = [
                ['Alias',             keystore.caTrustedAlias],
                ['MD5 Signature',     keystore.caTrustedSignature]
            ];

            vm.validation = [

            ]
        }

        //TODO: Good error handling
        function handleFailure(failure) {
            $log.error(JSON.stringify(failure));
        }
    }
})();