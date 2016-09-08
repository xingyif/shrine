/**
 * Created by ben on 2/12/15.
 */
angular.module("hms-authentication", ['ngCookies', 'hms-authentication-model'])
    .constant("UserRoles", {
        ROLE1: "Researcher",
        ROLE2: "DataSteward",
        ROLE3: "Admin"
    })
    .factory('HMSAuthenticationService', ['$http', '$q', '$app', 'HMSAuthenticationModel', '$rootScope', '$log',
        function ($http, $q, $app, model, $rootScope, $log) {

        var service     = {};

        service.Login = function () {
            return model.authenticate();
        };

        service.SetAuthHeader = function (username, password) {
            var authdata = $app.utils.toBase64(username + ':' + password);
            $http.defaults.headers.common['Authorization'] =  'Basic ' + authdata;
        };

        service.SetCredentials = function (username, password, roles) {
            var authdata = $app.utils.toBase64(username + ':' + password);
            $app.utils.setAppUser(username, authdata, roles);
        };

        service.ClearCredentials = function () {
            if($app.globals.currentUser !== undefined) {
                $app.globals.currentUser.isLoggedIn = false;
            }
            $app.utils.deleteAppUser();
            $http.defaults.headers.common.Authorization = ' Basic ';
        };

        // todo: -- auto logout on idle -- //
        var logoutSeconds = 2*60*100;
        var actionSeen = false;
        var intervalCalled = true;
        $rootScope.$watch(function detectIdle() {
            $log.warn("Detected a change in the root scope");
            if (intervalCalled) {
                intervalCalled = false;
            } else {
                actionSeen = true;
            }
        });

        $interval(function checkLogout() {
            if (!actionSeen) {
                $log.warn("I'd get called here!");
                clearCredentials();
            }
            actionSeen = false;
            intervalCalled = true;
            // -- forces the logout check, instead of waiting for user -- //
            $log.warn("Ping!");
        }, logoutSeconds);

        return service;
    }])
    .directive('restrict', function ($app) {
        return {
            restrict: 'A',
            priority: 100000,
            scope: {
                restrict: "@"
            },
            link: function ($scope, $element, $attrs) {
                var accessDenied,
                    user            = $app.globals.currentUser,
                    accessRoles     = $attrs.restrict.split(" ");

                //check if  matching roles.
                accessDenied = !$app.utils.hasAccess(user, accessRoles);

                if (accessDenied) {
                    $element.children().remove();
                    $element.remove();
                }
            }
        };
    });
