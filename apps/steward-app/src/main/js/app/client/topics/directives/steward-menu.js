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
            replace: true,
            //note: no controller needed for this directive.
            controller: function($scope){
                //added only to verify 'menu' element for dev.  remove.
            }
        };

        return stewardMenuDirective;
    }

})();