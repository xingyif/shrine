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
            $scope.config = config
            vm.config = config;
            $element.append($compile('<bootcordion data="vm.config"></bootcordion>')($scope));
        }
    }
})();
