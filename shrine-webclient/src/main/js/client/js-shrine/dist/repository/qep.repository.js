System.register(['aurelia-fetch-client', 'fetch'], function (_export, _context) {
    "use strict";

    var HttpClient, _createClass, _class, _temp, QEPRepository;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFetchClient) {
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

            _export('QEPRepository', QEPRepository = (_temp = _class = function () {
                function QEPRepository(http) {
                    var _this = this;

                    _classCallCheck(this, QEPRepository);

                    http.configure(function (config) {
                        config.useStandardConfiguration().withBaseUrl(_this.url).withDefaults({
                            headers: {
                                'Authorization': 'Basic ' + _this.auth
                            }
                        });
                    });
                    this.http = http;
                }

                QEPRepository.prototype.fetchPreviousQueries = function fetchPreviousQueries(limit) {
                    var skip = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 0;

                    return this.http.fetch('qep/queryResults?limit=' + limit + '&skip=' + skip, { method: 'get' }).then(function (response) {
                        return response.json();
                    }).catch(function (error) {
                        return error;
                    });
                };

                QEPRepository.prototype.fetchNetworkId = function fetchNetworkId(queryName) {
                    return this.http.fetch('qep/networkId?queryName=\'' + queryName + '\'', { method: 'get' }).then(function (response) {
                        return response.json();
                    }).catch(function (error) {
                        return error;
                    });
                };

                QEPRepository.prototype.fetchQuery = function fetchQuery(networkId, timeoutSeconds, afterVersion) {
                    return this.http.fetch('qep/queryResult/' + networkId + '?timeoutSeconds=' + timeoutSeconds + '&afterVersion=' + afterVersion, { method: 'get' }).then(function (response) {
                        var url = response.url,
                            statusText = response.statusText,
                            status = response.status,
                            ok = response.ok;

                        console.log('fetchQuery: ' + url + ' - ' + ok + ' - ' + status + ' - ' + statusText);
                        return response.json();
                    }).catch(function (error) {
                        return error;
                    });
                };

                QEPRepository.prototype.fetchStewardEmail = function fetchStewardEmail() {
                    return this.http.fetch('data?key=stewardEmail', { method: 'get' }).then(function (response) {
                        return response.json();
                    }).then(function (address) {
                        return address.indexOf('\"') > 0 ? address.split('\"')[1] : address;
                    }).catch(function () {
                        return '';
                    });
                };

                _createClass(QEPRepository, [{
                    key: 'url',
                    get: function get() {
                        var url = document.URL;
                        var service = ':6443/shrine-metadata/';
                        return url.substring(0, url.lastIndexOf(':')) + service;
                    }
                }, {
                    key: 'auth',
                    get: function get() {
                        var auth = sessionStorage.getItem('shrine.auth');
                        sessionStorage.removeItem('shrine.auth');
                        return auth;
                    }
                }]);

                return QEPRepository;
            }(), _class.inject = [HttpClient], _temp));

            _export('QEPRepository', QEPRepository);
        }
    };
});
//# sourceMappingURL=qep.repository.js.map
