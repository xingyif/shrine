(function () {
    'use strict';

    angular.module('shrine.steward')
        .directive('stewardMenu', StewardMenuDirective);

    function StewardMenuDirective() {

        var scope = {
            menu: '='
        };

        var stewardMenuDirective = {
            scope: scope,
            templateUrl: './app/client/topics/directives/steward-menu.tpl.html',
            restrict: 'E',
            replace: true
            // -- note: no controller necessary -- //
        };

        return stewardMenuDirective;
    }

})();