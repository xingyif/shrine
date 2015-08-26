'use strict';
angular.module('shrine-happy', ['happy-model', 'ngAnimate', 'ui.bootstrap'])
    .controller('HappyCtrl', ['$rootScope', '$scope', '$location', '$app', 'HappyMdl', function ($rootScope, $scope, $location, $app, model) {
        $scope.$app = $app;
        $scope.versionInfo          = {};
        $scope.adapter              = {};
        $scope.recentAuditEntries   = {};
        $scope.hiveConfig           = {};
        $scope.keystoreReport       = {};
        $scope.net                  = {};
        $scope.downstreamNodes      = {};
        $scope.recentQueries        = {};

        $scope.status = {

        };

        function reset() {
            $scope.versionInfo          = {};
            $scope.adapter              = {};
            $scope.recentAuditEntries   = {};
            $scope.hiveConfig           = {};
            $scope.keystoreReport       = {};
            $scope.net                  = {};
            $scope.downstreamNodes      = {};
            $scope.recentQueries        = {};
        }

        $scope.setAll = function () {
            reset();
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
                });
        };

        $scope.setKeystoreReport = function () {
            reset();
            model.getKeystore()
                .then(function (data) {
                    $scope.keystoreReport       = data.keystoreReport;
                });
        };

        $scope.setVersionInfo = function () {
            reset();
            model.getVersion()
                .then(function (data) {
                    $scope.versionInfo       = data.versionInfo;
                });
        };

        $scope.setAll();
    }])
    .directive("collapsible", function () {
        return {
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            restrict: 'A',
            templateUrl: "src/app/dashboard/apps/happy/collapsible.tpl.html",
            replace: true,
            controller: function ($scope, $element, $attrs) {
                $scope.collapsed = Boolean($attrs.collapsible === 'true');
            }
        };
    })
    .directive("versionInfo", function () {
        return {
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/version-info.tpl.html",
            replace: true,
            controller: function ($scope, $element, $attrs) {
            }
        };
    })
    .directive("recentQueries", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/recent-queries.tpl.html",
            replace: true,
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            controller: function ($scope, $element, $attrs, $app) {
                $scope.dateFormatter = function (timestamp) {
                    return $app.utils.utcToMMDDYYYY(Date.parse(timestamp));
                };
            }
        };
    })
    .directive("recentAuditEntries", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/recent-audit-entries.tpl.html",
            replace: true,
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            controller: function ($scope, $element, $attrs, $app) {
                $scope.dateFormatter = function (timestamp) {
                    return $app.utils.utcToMMDDYYYY(Date.parse(timestamp));
                };
            }
        };
    })
    .directive("network", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/network.tpl.html",
            replace: true,
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            controller: function ($scope, $element, $attrs, $app) {
            }
        };
    })
    .directive("keystoreReport", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/keystore-report.tpl.html",
            replace: true,
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            controller: function ($scope, $element, $attrs, $app) {
            }
        };
    })
    .directive("hiveConfig", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/hive-config.tpl.html",
            replace: true,
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            controller: function ($scope, $element, $attrs, $app) {
            }
        };
    })
    .directive("downstreamNodes", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/downstream-nodes.tpl.html",
            replace: true,
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            controller: function ($scope, $element, $attrs, $app) {
            }
        };
    })
    .directive("adapter", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/apps/happy/adapter.tpl.html",
            replace: true,
            scope: {
                ngModel:       '='
            },
            require: 'ngModel',
            controller: function ($scope, $element, $attrs, $app) {
            }
        };
    })


