(function () {
    'use strict';

    /**
     * Date methods.
     */
    angular
        .module('shrine.common')
        .factory('DateService', DateService);

    function DateService() {

        return {
            utcToDateString: utcToDateString,
            timestampToUtc: timestampToUtc,
            utcToMMDDYYYY: utcToMMDDYYYY,
            utcToTimeStamp: utcToTimeStamp
        };

        /**
        * Convert UTC to date string.
        * deprecated.
        * @todo: delete.
        * @param utc
        * @returns {string}
       */
        function utcToDateString(utc) {
            return new Date(utc).toDateString();
        }

        function timestampToUtc(timestamp) {
            return Date.parse(timestamp);
        }

        function utcToMMDDYYYY(utc) {

            //format data.
            var date = new Date(utc),
                format = 'MM/DD/YYYY',
                dd, mm, yyyy;

            //parse values from date
            dd = date.getDate();
            mm = date.getMonth() + 1;
            yyyy = date.getFullYear();

            format = format.replace(/DD/, dd)
                .replace(/MM/, mm)
                .replace(/YYYY/, yyyy);

            return format;
        }

        /**
         * Get a timestamp from a universal time code.
         * @param utc  universal time code.
         * @returns {string}
         */
        function utcToTimeStamp(utc) {

            //format data.
            var date = new Date(utc),
                format = 'MM/DD/YYYY HH:MN:SS',
                dd, mm, yyyy,
                mn, hh, ss, time, postfix;

            //parse values from date
            dd = date.getDate();
            mm = date.getMonth() + 1;
            yyyy = date.getFullYear();

            //parse values for time.
            hh = date.getHours();
            mn = date.getMinutes();
            ss = date.getSeconds();

            //format date.
            dd = (dd < 10) ? '0' + dd : dd;
            mm = (mm < 10) ? '0' + mm : mm;

            //format time.
            postfix = (hh > 12) ? ' PM' : ' AM';
            hh = ((hh < 10) ? '0' + hh : hh) % 12;
            mn = (mn < 10) ? '0' + mn : mn;
            ss = (ss < 10) ? '0' + ss : ss;

            format = format.replace(/DD/, dd)
                .replace(/MM/, mm)
                .replace(/YYYY/, yyyy)
                .replace(/HH/, hh)
                .replace(/MN/, mn)
                .replace(/SS/, ss);

            format += postfix;

            return format;
        }
    }
})();
