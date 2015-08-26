angular.module('app-utils', [])
    .service('AppUtilsService', function ($location) {

        var x2js = new X2JS();

        var svc = {

            isIDEA: function () {
                var absUrl   = $location.$$absUrl;
                return Boolean(absUrl.indexOf("localhost") > -1);
            },

            /**
             * Convert UTC to date string.
             * deprecated.
             * @param utc
             * @returns {string}
             */
            utcToDateString: function (utc) {
                return new Date(utc).toDateString();
            },


            timestampToUtc: function (timestamp){
                return Date.parse(timestamp);
            },

            utcToMMDDYYYY:  function (utc) {
                //format data.
                var date        = new Date(utc),
                    format      = 'MM/DD/YYYY',
                    dd, mm, yyyy;

                //parse values from date
                dd      =  date.getDate();
                mm      =  date.getMonth() + 1;
                yyyy    =  date.getFullYear();

                format  = format.replace(/DD/, dd)
                    .replace(/MM/, mm)
                    .replace(/YYYY/, yyyy);

                return format;
            },

            /**
             * Get a timestamp from a universal time code.
             * @param utc  universal time code.
             * @returns {string}
             */
            utcToTimeStamp: function (utc) {
                //format data.
                var date        = new Date(utc),
                    format      = 'MM/DD/YYYY HH:MN:SS',
                    dd, mm, yyyy,
                    mn, hh, ss, time, postfix;

                //parse values from date
                dd      =  date.getDate();
                mm      =  date.getMonth() + 1;
                yyyy    =  date.getFullYear();

                //parse values for time.
                hh  = date.getHours();
                mn  = date.getMinutes();
                ss  = date.getSeconds();

                //format date.
                dd  = (dd < 10) ? '0' + dd : dd;
                mm  = (mm < 10) ? '0' + mm : mm;

                //format time.
                postfix =  (hh > 12)? " PM" : " AM";
                hh = ((hh < 10)?'0' + hh : hh) % 12;
                mn = (mn < 10)? '0' + mn : mn;
                ss = (ss < 10)? '0' + ss : ss;



                format  = format.replace(/DD/, dd)
                    .replace(/MM/, mm)
                    .replace(/YYYY/, yyyy)
                    .replace(/HH/,hh)
                    .replace(/MN/, mn)
                    .replace(/SS/, ss);

                format += postfix;



                return format;
            },

            /**
             * Verify that the intersection of user roles and the roles array.
             * @param user - user object containing array of roles.
             * @param rolesArray - an array of acceptable roles.
             */
            hasAccess: function (user, rolesArray) {
                var hasAccess = (_.intersection(user.roles, rolesArray).length !== 0);
                return hasAccess;
            },

            /**
             * Convert a string to BASE 64.
             * @param str
             */
            toBase64: function (str) {
                return window.btoa(str);
            },

            /**
             *
             * @param xml
             * @returns {*}
             */
            xmlToJson: function (xml) {
                return x2js.xml_str2json(xml);
            }
        };

        return svc;
    });

