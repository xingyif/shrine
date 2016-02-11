(function () {
    'use strict';


    // -- angular module -- //
    angular
        .module('shrine.common')
        .value('UrlGetter', getUrl)// -- todo: move to another location? -- //
        .factory('UtilsService', UtilsService);


    // -- composable methods -- //
    /**
     * True if running from port utilized by IDEA
     * @returns {boolean}
     */
    function isTest() {
        return document.URL.indexOf('http://localhost:63342/') > -1
    }


    /**
     * Create url depending on local or deployment.
     * assumption is that remote rest structure is mimicked on local test folder.
     * @returns
     */
    function getUrl (endpoint) {

        // -- local -- //
        var testUrl     = 'test/',
            urlKey      = 'shrine-dashboard';

        // -- testing locally or deploy -- //
        return (isTest())?
            (testUrl + endpoint + '.json'): getDeployUrl(urlKey) + endpoint;
    }


    /**
     *
     * @param urlKey
     * @returns {string}
     */
    function getDeployUrl (urlKey) {

        // -- local vars. -- //
        var urlIndex    = 0,
            href        = '';

        // -- parse url from location.
        href        = document.location.href;
        urlIndex    = href.indexOf(urlKey) + urlKey.length;
        return href.substring(0, urlIndex) + '/';
    }


    /**
     * General Utility Sercice for Shrine Model.
     * @type {string[]}
     */
    UtilsService.$inject = ['XMLService', 'DataTypesService'];
    function UtilsService (xmlService, dataTypesService) {

        // -- constants -- //
        var deployUrl   = 'https://localhost:6443/shrine-dashboard/',
            testUrl     = 'test/';

        // -- public -- //
        return {
            isTest:           isTest,
            getUrl:           getUrl,
            hasAccess:        hasAccess,
            toBase64:         toBase64,
            xmlService:       xmlService,
            dataTypesService: dataTypesService
        }


        // -- private -- //
        /**
         *
         * @returns {boolean}
         */
        function isTest() {
            return document.URL.indexOf('http://localhost:63342/') > -1
        }


        /**
         * Verify that the intersection of user roles and the roles array.
         * @param user - user object containing array of roles.
         * @param rolesArray - an array of acceptable roles.
         */
        function hasAccess(user, rolesArray) {
            var hasAccess = (_.intersection(user.roles, rolesArray).length !== 0);
            return hasAccess;
        }


        /**
         * Convert a string to BASE 64.
         * @param str
         */
        function toBase64(str) {
            return window.btoa(str);
        }

    }
})();


