(function () {
    'use strict';


    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('ProblemsController', ProblemsController);


    /**
     *
     * @type {string[]}
     */
    ProblemsController.$inject = ['$app'];
    function ProblemsController ($app) {
        var vm = this;

        init();

        /**
         *
         */
        function init () {
            //todo
            $app.model.getProblems()
                .then(setProblems)
        }

        function setProblems(probs) {
            probs.problems.forEach(function(elem) {
                elem.showDetails = false;
            });
            vm.problems = probs.problems;
            vm.totalProbs = probs.size
        }

    }
})();
