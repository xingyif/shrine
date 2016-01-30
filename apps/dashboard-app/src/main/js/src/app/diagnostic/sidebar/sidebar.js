(function() {
    'use strict';

  // -- angular -- //
  angular.module('shrine-tools')
      .directive('sidebar', Sidebar);


  /**
  * - Directive Config -
  *
  */
  function Sidebar () {

    var sidebar = {
      templateUrl:  'src/app/diagnostic/sidebar/sidebar.tpl.html',
      restrict:     'E',
      replace:      true,
      link:         SidebarLinker,
      controller:   SidebarController,
      controllerAs: 'sbVM',
      scope: {
        summary: '='
      }
    };

    return sidebar;
  }


  /**
   *  - Controller -
   *
   */
  SidebarController.$inject = ['$scope','$app'];
  function SidebarController ($scope, svc) {

    // -- scope --//
    var _sbVM = this;
    _sbVM.summary = $scope.summary;
  }

  /**
   * Controller renamed to 'vm'
   * @param scope
   * @param el
   * @param attr
   * @param vm -- renamed to vm.
   */
  SidebarLinker.$inject = ['scope', 'element', 'attributes', 'sbVM']
  function SidebarLinker(scope, el, attr, vm) {

  }
})();


