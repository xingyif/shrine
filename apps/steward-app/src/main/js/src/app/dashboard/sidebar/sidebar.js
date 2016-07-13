'use strict';

/**
 * @ngdoc directive
 * @name izzyposWebApp.directive:adminPosHeader
 * @description
 * # adminPosHeader
 */

angular.module('stewardApp')
  .directive('sidebar',['$location',function() {
    return {
      templateUrl:'app/client/dashboard/sidebar/sidebar.tpl.html',
      restrict: 'E',
      replace: true,
      scope: {
      },
      controller:function($scope, $app){
        $scope.selectedMenu = '';
        $scope.collapseVar = 0;
        $scope.multiCollapseVar = 0;
        $scope.roles = $app.globals.UserRoles;
        
        $scope.check = function(x){
          
          if(x==$scope.collapseVar)
            $scope.collapseVar = 0;
          else
            $scope.collapseVar = x;
        };
        
        $scope.multiCheck = function(y){
          
          if(y==$scope.multiCollapseVar)
            $scope.multiCollapseVar = 0;
          else
            $scope.multiCollapseVar = y;
        };
      }
    }
  }]);
