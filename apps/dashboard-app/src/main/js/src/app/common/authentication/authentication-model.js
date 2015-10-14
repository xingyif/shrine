/**
* Authentication Model
* @author   Ben Carmen
* @date     04/02/2015
* Edit Log:
*    @todo: bdc -- 02-13-13 -- this and example of a todo.     
*/
(function () {
    'use strict';
    angular
        .module('hms-authentication-model', ['model-service'])
        .service('HMSAuthenticationModel', ['$http', '$q', 'ModelService', function ($http, $q, mdlSvc) {
            // -- private -- //
            // -- private -- //
            var model   = this,
                URLS    =  {
                    AUTHENTICATE: 'user/whoami'
                };
            
            function parseResult(result) {
                //reject promise on fail.
                if(result.data === "AuthenticationFailed") {
                    return $q.reject(response);
                }

                var response = {
                    success: true,
                    msg:     result.data.statusText,
                    userId:  result.data.userId,
                    roles:   result.data.roles
                };

                return response;
            }

            /*
             * Authenticate User - note this must be called after authentication header has been created.
             */
            model.authenticate = function () {
                var url = mdlSvc.url.base + URLS.AUTHENTICATE;
                return $http.get(url)
                    .then(parseResult, function (result) {
                        var response = {
                            success: false,
                            msg: "invalid login " + result.data.statusText
                        };
                        return $q.reject(response);
                    });
            };
        }]);
}());