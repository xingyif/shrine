(function () {
    'use strict';

    /*
        Simple mailto initialization.
    */

    $(document).ready(function () {
        init();
    });

    function init() {
        fetchStewardEmail(getUrl())
            .then(initMailTo)
            .then(hideLoading)
            .fail(notifyOfFailure);
    }

    function getUrl() {
        var port = '6443';
        var url = document.URL;
        var service = '/shrine-metadata/data?key=stewardEmail';
        return url.substring(0, url.indexOf(port) + port.length) + service;
    }

    function fetchStewardEmail(url) {
        return $.ajax(url);
    }

    function hideLoading() {
        $('.init-div').addClass('mailto-hidden');
    }

    function showContent() {
        $('.content').removeClass('mailto-hidden');
    }

    function initMailTo(address) {
        var sMailto = 'mailto:' + address.split('\"')[1];
        var sSubject = 'subject=' + encodeURIComponent('Question from a SHRINE User');
        var body = encodeURIComponent('Please enter the suggested information and your question. Your data steward will reply to this email.' +
        '\n\n***Never send patient information, passwords, or other sensitive information by email****' +
        '\nName:' +
        '\nTitle:' +
        '\nUser name (to log into SHRINE):' +
        '\nTelephone Number (optional):' +
        '\nPreferred email address (optional):' +
        '\n\nQuestion or Comment:');

        $('.mailto').on('click', function () {
            var sBody = 'body=' + body;
            var mail = sMailto + '?' + sSubject + '&' + sBody;
            window.top.location = mail;
        });

        showContent();
    }

    function notifyOfFailure(data) {
        var errorText = 'The Steward contact form is not configured for your site,' +
            ' please contact your site administration to enable this functionality.';

        $('.init-div').html('<div>' + errorText + '</div>');
    }
}());
