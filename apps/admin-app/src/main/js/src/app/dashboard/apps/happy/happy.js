'use strict';
angular.module('shrine-happy', ['happy-model', 'ngAnimate', 'ui.bootstrap'])
    .constant("HappyStates", {
        STATE1: "General",
        STATE2: "Keystore Report",
        STATE3: "Hub",
        STATE4: "Adapter"
    })
    .controller('HappyCtrl', ['$rootScope', '$scope', '$location', '$app', 'HappyMdl', 'HappyStates', function ($rootScope, $scope, $location, $app, model, states) {
        $scope.$app = $app;
        $scope.versionInfo          = {};
        $scope.adapter              = {};
        $scope.recentAuditEntries   = {};
        $scope.hiveConfig           = {};
        $scope.keystoreReport       = {};
        $scope.net                  = {};
        $scope.downstreamNodes      = {};
        $scope.recentQueries        = {};
        $scope.states               = states;
        $scope.state                = $scope.states.STATE1;
        $scope.general = {
            keystoreOk:     true,
            hubOk:          true,
            adapterOk:      true
        }

        $scope.setAll = function () {
            model.getAll()
                .then(function (data) {
                    $scope.adapter              = data.all.adapter;
                    $scope.downstreamNodes      = data.all.downstreamNodes;
                    $scope.hiveConfig           = data.all.hiveConfig;
                    $scope.keystoreReport       = data.all.keystoreReport;
                    $scope.net                  = data.all.net;
                    $scope.recentAuditEntries   = data.all.recentAuditEntries;
                    $scope.recentQueries        = data.all.recentQueries;
                    $scope.versionInfo          = data.all.versionInfo;
                    $scope.general.keystoreOk   = true;
                    $scope.general.adapterOk    = $scope.adapter.result.response.errorResponse === undefined;
                    $scope.net.hasFailures      = Number($scope.net.failureCount) > 0;
                    $scope.net.hasInvalidResults = (Number($scope.net.validResultCount) < Number($scope.net.expectedResultCount));
                    $scope.net.hasTimeouts      = Number($scope.net.timeoutCount);
                    $scope.general.hubOk        = !($scope.net.hasFailures || $scope.net.hasInvalidResults || $scope.net.hasTimeouts)
                });
        };

        $scope.setStateAndRefresh = function (state) {
            $scope.state = state;
        }

        $scope.setStateAndRefresh(states.STATE1);
        $scope.setAll();
    }])
    .directive("general", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/general/general.tpl.html",
            replace: true
        };
    })
    .directive("keystoreReport", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/keystore-report/keystore-report.tpl.html",
            replace: true
        };
    })
    .directive("hub", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/hub/hub.tpl.html",
            replace: true
        };
    })
    .directive("adapter", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/adapter/adapter.tpl.html",
            replace: true
        };
    });




