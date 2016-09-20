(function () {
    angular.module('shrine.steward')
        .directive('menu', MenuDirective);

    function MenuDirective() {
        var menu = {
            restrict: 'E',
            templateUrl: './app/client/directives/menu.tpl.html',
            scope: {
                user: '='
            }
        };

        return menu;
    }
})();
