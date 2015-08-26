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
angular.module('shrine-tools')
    .directive('hmsFooter',function(){
        return {
            templateUrl:'src/app/dashboard/footer/footer.tpl.html',
            restrict: 'E',
            replace: true
        }
    });
