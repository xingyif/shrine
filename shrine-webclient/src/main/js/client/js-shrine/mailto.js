(function () {
    'use strict';

    var sMailto = 'mailto:${stewardEmail}';
    var sSubject = 'subject=' + encodeURIComponent('Question from a SHRINE User');
    var body = encodeURIComponent('Please enter the suggested information and your question. Your data steward will reply to this email.' +
        '\n\n***Never send patient information, passwords, or other sensitive information by email****' +
        '\nName:' +
        '\nTitle:' +
        '\nUser name (to log into SHRINE):' +
        '\nTelephone Number (optional):' +
        '\nPreferred email address (optional):' +
        '\n\nQuestion or Comment:');

    var sBody = 'body=' + body;
    var mail = sMailto + '?' + sSubject + '&' + sBody;


    /*
        Simple mailto initialization.
    */

    $(document).ready(function () {
        init();
    });

    function init() {
        $('.mailto').on('click', function () {
            fetchStewardEmail(getUrl())
                .then(setStewardEmail)
                .fail(setEmailToEmpty)
                .always(sendToBrowser);
        });

        hideLoading();
        showContent();
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

    function setStewardEmail(address) {
        var stewardEmail = address.split('\"')[1];
        mail = mail.replace('${stewardEmail}', stewardEmail);
    }

    function setEmailToEmpty() {
        mail = mail.replace('${stewardEmail}', '');
    }

    function sendToBrowser() {
        window.top.location = mail;
    }
}());
