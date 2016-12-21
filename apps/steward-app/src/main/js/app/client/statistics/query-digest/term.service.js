(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .service('TermService', TermServiceFactory);

    function TermServiceFactory() {
        return {
            getPct: getPct,
            getStyleByFrequency: getStyleByFrequency
        };

        function getPct(queryCount, maxCount) {
            var maxPct = 100;
            var result = Math.ceil((queryCount / maxCount) * maxPct);
            return (result > maxPct) ? maxPct : result;
        }

        function getStyleByFrequency(queryCount, maxCount) {
            var frequency = queryCount / maxCount;

            if (frequency < 0.20) {
                return 'blue';
            }
            else if (frequency < 0.40) {
                return 'dark-blue';
            }
            else if (frequency < 0.60) {
                return 'purple'
            }
            else if (frequency < 0.80) {
                return 'red'
            }
            else {
                return 'dark-red';
            }
        }
    }

})();
