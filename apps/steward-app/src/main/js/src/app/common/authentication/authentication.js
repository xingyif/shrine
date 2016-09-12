/**
 * Created by ben on 2/12/15.
 */
angular.module("hms-authentication", ['ngCookies', 'hms-authentication-model'])
    .constant("UserRoles", {
        ROLE1: "Researcher",
        ROLE2: "DataSteward",
        ROLE3: "Admin"
    })
    .factory('HMSAuthenticationService', ['$http', '$q', '$app', 'HMSAuthenticationModel', '$rootScope', '$log', '$location', '$interval',
        function ($http, $q, $app, model, $rootScope, $log, $location, $interval) {

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

            idleHandle();

            function idleHandle() {
                // -- auto logout on idle -- //
                var twentyMinutes = 5000;
                var logoutPromise = $interval(timeout, twentyMinutes);
                var idleEvent = 'idleEvent';
                $rootScope.$on('$destroy', function () {
                    $interval.cancel(logoutPromise);
                });
                $rootScope.$on(idleEvent, function () {
                    $log.warn('heyo!');
                    $interval.cancel(logoutPromise);
                    logoutPromise = $interval(timeout, twentyMinutes);
                });


                /**
                 * When the interval is called, that means the user has gone idle, so we
                 * clear their credentials then navigate them back to the home page.
                 */
                function timeout() {
                    service.ClearCredentials();
                    $location.url("/login");
                }

                $rootScope.idleBroadcast = function() {
                    $rootScope.$broadcast(idleEvent);
                    $log.warn('hello');
                }
            }

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
