(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('ShrineConfigurationController', ShrineConfigurationController);

    ShrineConfigurationController.$inject = ['$scope','$app', '$element', '$compile'];
    function ShrineConfigurationController ($scope, $app, $element, $compile) {
        var vm      = this;
        $scope.ready    = 'false';

        init();

        /**
         *
         */
        function init () {
            $app.model.getConfig()
                .then(setConfig, handleFailure)
        }

        /**
         *
         * @param config
         */
        function setConfig (config) {
                $scope.config = config;
                vm.config = config;
                $element.append($compile('<bootcordion data="vm.config"></bootcordion>')($scope));
        }

        function handleFailure(configFailure) {
            vm.configError = configFailure;
            $element.append($compile('<h3>Config received an error response:</h3>'));
            $element.append($compile('<span>{{vm.configError}}</span>'));
        }
    }
})();
