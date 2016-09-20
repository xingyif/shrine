(function () {
    'use strict';


    // -- register controller with angular -- //
    angular.module('shrine-tools', ['ui.bootstrap', 'ui.bootstrap.datepicker'])
        .controller('ProblemsController', ProblemsController)
        .directive('myPagination', function () {
            return {
                restrict:  'A',
                replace: true,
                templateUrl: 'src/app/diagnostic/templates/my-pagination-template.html'
            }
        })
        .directive('myPages', function() {

            function rangeGen(max) {
                var result = [];
                for (var i = 1; i < max - 1; i++) {
                    result[i] = i + 1;
                }
                return result;
            }

            function abs(num) {
                if (num < 0) {
                    return num * -1;
                } else {
                    return num;
                }
            }

            function checkPage(value, activePage, maxPage, minPage) {
                if (maxPage == minPage) {
                    return false;
                } else if (maxPage - minPage <= 5) {
                    return isFinite(value) && value <= maxPage && value >= minPage;
                } else if (value == maxPage || value == minPage || value == activePage || value == '‹' || value == '›') {
                    return true;
                } else if (value == "..") {
                    return activePage > 5;
                } else if (value == "...") {
                    return maxPage - activePage > 4;
                } else if (value < activePage) {
                    return activePage <= 5 || activePage - value <= 2;
                } else if (value > activePage) {
                    return maxPage - activePage <= 4 || value - activePage <= 2;
                }
            }

            return {
                restrict: 'E',
                templateUrl: 'src/app/diagnostic/templates/paginator-template.html',
                scope: {
                    maxPage:      '=',
                    minPage:      '=',
                    handleButton: '=',
                    activePage:   '='
                },
                link: function(scope) {
                    scope.rangeGen = rangeGen;
                    scope.checkPage = checkPage;
                }
            }
        });

    ProblemsController.$inject = ['$app', '$log', '$sce'];
    function ProblemsController ($app, $log, $sce) {
        var vm = this;

        init();

        /**
         *
         */
        function init () {
            //vm.date = new Date();
            vm.isOpen = { 'open': false };
            vm.pageSizes = [5, 10, 20];
            vm.url = "https://open.med.harvard.edu/wiki/display/SHRINE/";
            vm.submitDate = submitDate;
            vm.newPage = newPage;
            vm.floor = Math.floor;
            vm.handleButton = handleButton;
            vm.showP =         function(target)  { return vm.probsSize > target};
            vm.checkDate =     function(date)    { return date != undefined };
            vm.open =          function()        { vm.isOpen.open = !vm.isOpen.open;};
            vm.pageSizeCheck = function(n)       { return n < vm.probsSize };
            vm.parseDetails =  function(details) { return $sce.trustAsHtml(parseDetails(details)) };
            vm.numCheck =      function(any)     { return isFinite(any)? (any - 1) * vm.probsN: vm.probsOffset };
            vm.changePage =    function()        { vm.newPage(vm.probsOffset, vm.pageSize > 20? 20: vm.pageSize < 0? 0: vm.pageSize) };

            //todo: Get rid of this and figure out something less hacky
            vm.formatCodec = function(word) {
                var index = word.lastIndexOf('.');
                var arr = word.trim().split("");
                arr[index] = '\n';
                return arr.join("");
            };

            newPage(0, 20)
        }

        function handleButton(value) {
            var page = function(offset) { newPage(offset, vm.probsN) };
            switch(value) {
                case '..':
                    break;
                case '‹':
                    page(vm.probsOffset - vm.probsN);
                    break;
                case '›':
                    page(vm.probsOffset + vm.probsN);
                    break;
                case '...':
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

        function submitDate() {
            var epoch = vm.date.getTime() + 86400000; // + a day
            vm.showDateError = false;
            newPage(vm.probsOffset, vm.probsN, epoch);
        }


        function newPage(offset, n, epoch) {
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
            var num = floorMod(clamp(offset), vm.probsN);
            $app.model.getProblems(num, n, epoch)
                .then(setProblems)
        }

        function setProblems(probs) {
            vm.problems = probs.problems;
            vm.probsSize = probs.size;
            vm.probsOffset = probs.offset;
            vm.probsN = probs.n;
            vm.pageSize = vm.probsN;
        }

        function parseDetails(detailsObject) {
            var detailsTag = '<h3>details</h3>';
            var detailsField = detailsObject['details'];
            if (detailsField === '') {
                return '<h3>No details associated with this problem</h3>'
            } else if (typeof(detailsField) === 'string') {
                return detailsTag + '<p>'+sanitizeString(detailsField)+'</p>';
            } else if (typeof(detailsField) === 'object' && 'exception' in detailsField) {
                return detailsTag + parseException(detailsField['exception']);
            } else {
                return detailsTag + '<pre>'+sanitizeString(JSON.stringify(detailsField))+'</pre>'
            }
        }

        function parseException(exceptionObject) {
            var exceptionTag = '<h4>exception</h4>';
            var nameTag = '<h5>'+sanitizeString(exceptionObject['name'])+'</h5>';
            var messageTag = '<p>'+sanitizeString(exceptionObject['message'])+'</p>';
            var stackTrace = exceptionObject['stacktrace'];
            return exceptionTag + nameTag + messageTag + parseStackTrace(stackTrace);
        }

        function parseStackTrace(stackTraceObject) {
            if (stackTraceObject.hasOwnProperty('exception')) {
                return '<p>'+sanitizeString(stackTraceObject['line'])+'</p>' + parseException(stackTraceObject['exception']);
            } else {
                return '<h4>stack trace</h4>' + parseLines(stackTraceObject['line']);
            }
        }

        function parseLines(lineArray) {
            var result = '<p>';
            for (var i =0; i < lineArray.length; i++) {
                result += sanitizeString(lineArray[i]) + '<br>';
            }
            result += '</p>';
            return result;
        }

        function sanitizeString(str) {
            var chars = str.split('');
            var escapes = {
                '<': '&#60;', '>': '&#62;', '&': '&#38;', '"': '&#34;',
                "'": '&#39;', ' ': '&#32;', '!': '&#33;', '@': '&#64;',
                '$': '&#36;', '%': '&#37;', '(': '&#40;', ')': '&#41;',
                '=': '&#61;', '+': '&#43;', '{': '&#123;', '}': '&#125;',
                '[': '&#91;', ']': '&#93;'
            };
            for (var i = 0; i < chars.length; i++) {
                var c = chars[i];
                if (escapes.hasOwnProperty(c)) {
                    chars[i] = escapes[c]
                }
            }
            return chars.join('');
        }

    }
})();
