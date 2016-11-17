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
      controllerAs: 'vm',
      scope: {
        options: '='
      }
    };

    return sidebar;
  }


  /**
   *  - Controller -
   *
   */
  SidebarController.$inject = ['$scope','$app'];
  function SidebarController ($scope, $app) {

    // -- scope --//
    var vm = this;

    init();

    function init() {
      $app.model.getOptionalParts()
          .then(setOptions);
    }



    function setOptions(data) {
      vm.options = data;
    }

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


