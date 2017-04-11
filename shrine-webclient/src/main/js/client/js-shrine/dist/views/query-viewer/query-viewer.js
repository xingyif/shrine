System.register(['aurelia-framework', 'views/query-viewer/query-viewer.service'], function (_export, _context) {
    "use strict";

    var inject, QueryViewerService, _createClass, _dec, _class, nodesPerScreen, QueryViewer;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_viewsQueryViewerQueryViewerService) {
            QueryViewerService = _viewsQueryViewerQueryViewerService.QueryViewerService;
        }],
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

            nodesPerScreen = 10;

            _export('QueryViewer', QueryViewer = (_dec = inject(QueryViewerService), _dec(_class = function () {
                function QueryViewer(service) {
                    var _this = this;

                    _classCallCheck(this, QueryViewer);

                    this.screenIndex = 0;
                    this.service = service;
                    this.service.fetchPreviousQueries().then(function (result) {
                        var queries = result.queries;
                        var nodes = _this.service.getNodes(queries);
                        return service.getScreens(nodes, queries);
                    }).then(function (screens) {
                        _this.screens = screens;
                    }).catch(function (error) {
                        return console.log(error);
                    });
                }

                QueryViewer.prototype.isUnresolved = function isUnresolved(status) {
                    return ['ERROR', 'PENDING'].indexOf(status) >= 0;
                };

                QueryViewer.prototype.getStatusStyle = function getStatusStyle(status) {
                    if (status === 'ERROR') {
                        return '#FF0000';
                    } else if (status === 'PENDING') {
                        return '#00FF00';
                    }

                    return '';
                };

                _createClass(QueryViewer, [{
                    key: 'slidePct',
                    get: function get() {
                        return String(-100 * this.screenIndex) + '%';
                    }
                }]);

                return QueryViewer;
            }()) || _class));

            _export('QueryViewer', QueryViewer);
        }
    };
});
//# sourceMappingURL=query-viewer.js.map
