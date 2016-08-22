(function () {
    'use strict';


    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('ProblemsController', ProblemsController)
        .directive('myPagination', function () {
            return {
                restrict:  'A',
                replace: true,
                template: '<tr>' +
                '<td colspan="4" style="width:100%;text-align:center">' +
                '<h5 style="display:inline-block;float:left;font-weight: bolder">' +
                '<span style="cursor:pointer" ng-click="vm.alert(vm);vm.newPage(vm.probsOffset - vm.probsN, vm.probsN)">' +
                '&#8592;' +
                '</span>' +
                '|' +
                '<span style="cursor:pointer" ng-click="vm.newPage(vm.probsOffset + vm.probsN, vm.probsN)">' +
                '&#8594;' +
                '</span>' +
                '</h5>' +
                '<h5 style="display:inline-block">' +
                '{{Math.floor(vm.probsOffset / vm.probsN) + 1}} / {{Math.floor(vm.probsSize / vm.probsN) + 1}}' +
                '</h5>' +
                '<form style="display:inline-block;float:right" ng-submit="vm.newPage(vm.numCheck(vm.num), vm.probsN)">' +
                '<label>' +
                'Go to page:' +
                '<input type="number" name="vm.num" ng-model="vm.num">' +
                '</label>' +
                '</form>' +
                '</td>' +
                '</tr>'
            }
        });

    ProblemsController.$inject = ['$app', '$window']; //, '$log'];
    function ProblemsController ($app, $window) {
        var vm = this;

        init();

        /**
         *
         */
        function init () {
            vm.num = 0;
            vm.url = "https://open.med.harvard.edu/wiki/display/SHRINE/";
            vm.newPage = newPage;
            vm.floorMod = floorMod;
            vm.numCheck = function(any) {return isFinite(any)? (any - 1) * vm.probsN: vm.probsOffset};
            vm.formatCodec = function(word) {
                var index = word.lastIndexOf('.');
                var arr = word.trim().split("");
                arr[index] = '\n';
                return arr.join("");
            };
            vm.min = Math.min;
            vm.stringify = function(arg) { return JSON.stringify(arg, null, 2); };
            vm.alert = function(arg) {
                arg = JSON.stringify(arg, null, 2);
                $window.alert(arg)
            };
            newPage(0)
        }

        function floorMod(num1, num2) {
            if (!(num1 && num2)) {
                // can't mod without real numbers
                return num1;
            } else {
                var n1 = Math.floor(num1);
                var n2 = Math.floor(num2);
                return n1 - (n1 % n2);
            }
        }


        function newPage(offset, n) {
            if (!n || !isFinite(n)) {
                n = 20;
            }
            if (!isFinite(offset)) {
                return;
            }
            var clamp = function(num1) {
                if (!vm.probsSize) {
                    // Can't clamp, since probsSize isn't set yet.
                    return num1;
                } else {
                    return Math.max(0, Math.min(vm.probsSize, num1));
                }
            };
            var num = vm.floorMod(clamp(offset), vm.probsN);
            $app.model.getProblems(num, n)
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
