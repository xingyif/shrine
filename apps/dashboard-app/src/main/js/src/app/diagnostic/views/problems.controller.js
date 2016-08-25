(function () {
    'use strict';


    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('ProblemsController', ProblemsController)
        .directive('myPagination', function () {
            return {
                restrict:  'A',
                replace: true,
                templateUrl: 'src/app/diagnostic/templates/my-pagination-template.html'
            }
        }).directive('myPages', function() {

        function rangeGen(max) {
            var result = [];
            for (var i = 0; i < max; i++) {
                result[i] = i + 1;
            }
            return result;
        }


        function checkPage(value, activePage, maxPage) {
            if (!isFinite(value)) {
                // Anything that's not a number and not an error is fine as a button
                return !!value;
            } else if (activePage == 1 || activePage == 2) {
                return value <= 4;
            } else if (activePage == maxPage || activePage == maxPage - 1) {
                return maxPage - value < 4;
            } else {
                var diff = value - activePage;
                return diff >= -2 && diff < 2;
            }
        }

        return {
            restrict: 'E',
            templateUrl: 'src/app/diagnostic/templates/pages.html',
            scope: {
                maxPage:      '=',
                handleButton: '=',
                activePage:   '='
            },
            link: function(scope) {
                scope.rangeGen = rangeGen;
                scope.checkPage = checkPage;
            }
        }
    });

    ProblemsController.$inject = ['$app', '$log'];
    function ProblemsController ($app, $log) {
        var vm = this;

        init();

        /**
         *
         */
        function init () {
            vm.num = 0;
            vm.showDateError = false;
            vm.submitDate = submitDate;
            vm.url = "https://open.med.harvard.edu/wiki/display/SHRINE/";
            vm.newPage = newPage;
            vm.floorMod = floorMod;
            vm.floor = Math.floor;
            vm.handleButton = handleButton;
            vm.numCheck = function(any) {return isFinite(any)? (any - 1) * vm.probsN: vm.probsOffset};
            vm.formatCodec = function(word) {
                var index = word.lastIndexOf('.');
                var arr = word.trim().split("");
                arr[index] = '\n';
                return arr.join("");
            };
            vm.min = Math.min;
            vm.stringify = function(arg) { return JSON.stringify(arg, null, 2); };
            newPage(0, 20)
        }

        function handleButton(value) {
            var page = function(offset) { newPage(offset, vm.probsN) };
            switch(value) {
                case '«':
                    page(0);
                    break;
                case '‹':
                    page(vm.probsOffset - vm.probsN);
                    break;
                case '›':
                    page(vm.probsOffset + vm.probsN);
                    break;
                case '»':
                    page(vm.probsSize);
                    break;
                default:
                    page((value - 1) * vm.probsN);
            }
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

        // todo
        function submitDate(dateString) {
            if (checkDate(dateString)) {
                var epoch = new Date(dateString).getTime() + 86400000; // + a day
                vm.showDateError = false;
                newPage(vm.probsOffset, vm.probsN, epoch);
            } else {
                vm.showDateError = true;
            }
        }

        function checkDate(dateString) {
            try {
                var split = dateString.split("-");
                var month = parseInt(split[1]);
                var day = parseInt(split[2]);
                var year = parseInt(split[0]);
                return split.length == 3 && month <= 12 && month >= 1 &&
                    year >= 0 && validDay(day, month, year);
            } catch (err) {
                return false;
            }
        }

        function validDay(day, month, year) {
            var thirtyOne = [1, 3, 5, 7, 8, 10, 12];
            var thirty    = [4, 6, 9, 11];

            if (contains(thirtyOne, month)) {
                return day <= 31 && day >= 1;
            } else if (contains(thirty, month)) {
                return day <= 30 && day >= 1;
            } else if (month == 2 && year % 4 == 0 && (!(year % 100 == 0) || year % 400 == 0)) {
                // Leap year is every year that is divisible by 4. If it's also divisible by 100, then it's only
                // A leap year if it is also divisible by 400.
                return day <= 29 && day >= 1;
            } else {
                return day <= 28 && day >= 1;
            }

        }

        function contains(arr, elem) {
            for (var i = 0; i < arr.length; i++) {
                if (arr[i] === elem) {
                    return true;
                }
            }
            return false;
        }


        function newPage(offset, n, epoch) {
            if (!(epoch && isFinite(epoch))) {
                epoch = -1;
            }
            if (!(n && isFinite(n))) {
                n = 20;
            }
            if (!(offset && isFinite(offset))) {
                offset = 0;
            }
            var clamp = function(num1) {
                if (!vm.probsSize) {
                    // Can't clamp, since probsSize isn't set yet.
                    return num1;
                } else {
                    return Math.max(0, Math.min(vm.probsSize - 1, num1));
                }
            };
            var num = vm.floorMod(clamp(offset), vm.probsN);
            $app.model.getProblems(num, n, epoch)
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
