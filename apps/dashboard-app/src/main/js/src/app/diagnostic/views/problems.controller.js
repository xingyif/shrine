(function () {
    'use strict';


    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('ProblemsController', ProblemsController);


    /**
     *
     * @type {string[]}
     */
    ProblemsController.$inject = ['$app', '$log'];
    function ProblemsController ($app, $log) {
        var vm = this;

        init();

        /**
         *
         */
        function init () {
            vm.num = 0;
            //vm.probsN = 20;
            vm.newPage = newPage;
            vm.floorMod = function(num1, num2) {
                if (!(num1 && num2)) {
                    // can't mod without real numbers
                    return num1;
                } else {
                    var n1 = Math.floor(num1);
                    var n2 = Math.floor(num2);
                    return n1 - (n1 % n2);
                }
            };
            vm.min = Math.min;
            vm.stringify = function(arg) { return JSON.stringify(arg, null, 2); };
            newPage(0)
        }

        function newPage(n) {
            if (!isFinite(n)) {
                $log.error('n is not defined!');
            }
            var clamp = function(num1) {
                if (!vm.probsSize) {
                    // Can't clamp, since probsSize isn't set yet.
                    return num1;
                } else {
                    return Math.max(0, Math.min(vm.probsSize, num1));
                }
            };

            var num = vm.floorMod(clamp(n), vm.probsN);
            $app.model.getProblems(num)
                .then(setProblems)
        }

        function setProblems(probs) {
            vm.problems = probs.problems;
            vm.probsSize = probs.size;
            vm.probsOffset = probs.offset;
            vm.probsN = probs.n;
        }

    }
})();
