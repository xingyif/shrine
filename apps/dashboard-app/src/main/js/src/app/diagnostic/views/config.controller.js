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
                .then(setConfig)
        }

        /**
         *
         * @param configuration
         */
        function setConfig (config) {
            if (!config.hasOwnProperty('failed')) {
                $scope.config = config;
                vm.config = config;
                $element.append($compile('<bootcordion data="vm.config"></bootcordion>')($scope));
            } else {
                vm.configError = config.failed;
                $element.append($compile('<h3>Config received an error response:</h3>'));
                $element.append($compile('<span>{{vm.configError}}</span>'));
            }
        }
    }
})();
