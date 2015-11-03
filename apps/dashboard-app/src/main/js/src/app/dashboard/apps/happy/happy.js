'use strict';
angular.module('shrine-happy', ['happy-model', 'ngAnimate', 'ui.bootstrap'])
    .constant("HappyStates", {
        STATE0: "Loading",
        STATE1: "General",
        STATE2: "Keystore",
        STATE3: "Hub",
        STATE4: "Adapter",
        STATE5: "QEPFailure"
    })
    .controller('HappyCtrl', ['$rootScope', '$scope', '$location', '$app', 'HappyMdl', 'HappyStates', '$sce', function ($rootScope, $scope, $location, $app, model, states, $sce) {
        $scope.$app = $app;
        $scope.versionInfo          = {};
        $scope.adapter              = {};
        $scope.recentAuditEntries   = {};
        $scope.hiveConfig           = {};
        $scope.keystore             = {};
        $scope.net                  = {};
        $scope.downstreamNodes      = {};
        $scope.recentQueries        = {};
        $scope.QEPError             = "";
        $scope.states               = states;
        $scope.state                = $scope.states.STATE0;
        $scope.general = {
            keystoreOk:     true,
            hubOk:          true,
            adapterOk:      true,
            isHub:          false
        }

        $scope.setAll = function () {
            model.getAll()
                .then(function (data) {
                    $scope.adapter              = data.all.adapter;
                    $scope.hiveConfig           = data.all.hiveConfig;
                    $scope.keystore             = data.all.keystoreReport;

                    //setting for net.
                    $scope.recentAuditEntries   = data.all.recentAuditEntries;
                    $scope.recentQueries        = data.all.recentQueries;
                    $scope.versionInfo          = data.all.versionInfo;
                    $scope.general.keystoreOk   = true;
                    $scope.general.adapterOk    = $scope.adapter.result.response.errorResponse === undefined;

                    // - if not a hub, then can we assume that 'net' will not be an
                    // element on the data object?
                    $scope.general.isHub        = data.all.notAHub === undefined;

                    if($scope.general.isHub === true) {
                        $scope.net                      = data.all.net;
                        $scope.downstreamNodes      = data.all.downstreamNodes;
                        $scope.net.hasFailures          = Number($scope.net.failureCount) > 0;
                        $scope.net.hasInvalidResults    = (Number($scope.net.validResultCount) < Number($scope.net.expectedResultCount));
                        $scope.net.hasTimeouts          = Number($scope.net.timeoutCount);
                    }

                    $scope.general.hubOk = !$scope.general.isHub ||
                        !($scope.net.hasFailures || $scope.net.hasInvalidResults || $scope.net.hasTimeouts)
                    $scope.setStateAndRefresh(states.STATE1);
                },
                function (data) {
                    $scope.trustedHtml= $sce.trustAsHtml(data);
                   $scope.setStateAndRefresh(states.STATE5);

                }
            );
        };

        $scope.setStateAndRefresh = function (state) {
            $scope.state = state;
        }
        $scope.setAll();
    }])
    .directive("general", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/general/general.tpl.html",
            replace: true
        };
    })
    .directive("keystore", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/keystore/keystore.tpl.html",
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
    })
    .directive("fatalFailure", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/fatal-failure/fatal-failure.tpl.html",
            replace: true
        };
    });




