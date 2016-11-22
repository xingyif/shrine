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
    vm.toDashboard = $app.model.toDashboard;

    init();

    function init() {
      $app.model.getOptionalParts()
          .then(setOptions);

      $app.model.getQep()
          .then(setQep);

      vm.hasHub = function() {return hasHub(vm.trustModelIsHub, vm.options.isHub, vm.toDashboard.url)};
    }



    function setOptions(data) {
      vm.options = data;
    }

    function setQep(data) {
      vm.trustModelIsHub = data.trustModelIsHub;
    }

    function hasHub(trustModelIsHub, isHub, toDashboardUrl) {
      return !trustModelIsHub || isHub && toDashboardUrl == '';
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


