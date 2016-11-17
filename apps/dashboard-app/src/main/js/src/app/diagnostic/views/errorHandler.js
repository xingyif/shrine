( function () {


    // -- register directive with angular  -- //
    angular.module('shrine-tools')
        .directive('errorHandler', errorHandler);

    function errorHandler () {
        return {
            restrict: 'E',
            templateUrl: 'src/app/diagnostic/templates/error-handler-template.html',
            scope: {error: '=', errorName: '='}
        }
    }
})();