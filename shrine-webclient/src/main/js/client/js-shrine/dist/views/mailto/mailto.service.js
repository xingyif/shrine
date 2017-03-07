System.register(['aurelia-framework', 'aurelia-fetch-client', 'fetch'], function (_export, _context) {
    "use strict";

    var inject, HttpClient, _createClass, _dec, _class, MailToService;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_aureliaFetchClient) {
            HttpClient = _aureliaFetchClient.HttpClient;
        }, function (_fetch) {}],
        execute: function () {
            _createClass = function () {
                function defineProperties(target, props) {
                    for (var i = 0; i < props.length; i++) {
                        var descriptor = props[i];
                        descriptor.enumerable = descriptor.enumerable || false;
                        descriptor.configurable = true;
                        if ("value" in descriptor) descriptor.writable = true;
                        Object.defineProperty(target, descriptor.key, descriptor);
                    }
                }

                return function (Constructor, protoProps, staticProps) {
                    if (protoProps) defineProperties(Constructor.prototype, protoProps);
                    if (staticProps) defineProperties(Constructor, staticProps);
                    return Constructor;
                };
            }();

            _export('MailToService', MailToService = (_dec = inject(HttpClient), _dec(_class = function () {
                function MailToService(http) {
                    var _this = this;

                    _classCallCheck(this, MailToService);

                    http.configure(function (config) {
                        config.useStandardConfiguration().withBaseUrl(_this.url);
                    });

                    this.http = http;
                }

                MailToService.prototype.fetchStewardEmail = function fetchStewardEmail() {
                    return this.http.fetch('data?key=stewardEmail').then(function (response) {
                        return response.json();
                    }).then(function (address) {
                        return address.indexOf('\"') > 0 ? address.split('\"')[1] : address;
                    }).catch(function () {
                        return '';
                    });
                };

                _createClass(MailToService, [{
                    key: 'url',
                    get: function get() {
                        var port = '6443';
                        var url = document.URL;
                        var service = '/shrine-metadata/';
                        return url.substring(0, url.indexOf(port) + port.length) + service;
                    }
                }]);

                return MailToService;
            }()) || _class));

            _export('MailToService', MailToService);
        }
    };
});
//# sourceMappingURL=mailto.service.js.map
