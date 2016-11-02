/**
 * Created by ben on 6/15/15.
 */
'use strict';

/**
 * @ngdoc directive
 * @name izzyposWebApp.directive:adminPosHeader
 * @description
 * # adminPosHeader
 */
angular.module('stewardApp')
    .directive('hmsFooter',function(){
        return {
            templateUrl:'app/client/footer/footer.tpl.html',
            restrict: 'E',
            replace: true
        }
    });
