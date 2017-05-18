System.register(['aurelia-framework', 'views/query-viewer/query-viewer.service', 'common/i2b2.service.js', './query-viewer.model'], function (_export, _context) {
    "use strict";

    var inject, computedFrom, QueryViewerService, I2B2Service, QueryViewerModel, _dec, _class, QueryViewer;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
            computedFrom = _aureliaFramework.computedFrom;
        }, function (_viewsQueryViewerQueryViewerService) {
            QueryViewerService = _viewsQueryViewerQueryViewerService.QueryViewerService;
        }, function (_commonI2b2ServiceJs) {
            I2B2Service = _commonI2b2ServiceJs.I2B2Service;
        }, function (_queryViewerModel) {
            QueryViewerModel = _queryViewerModel.QueryViewerModel;
        }],
        execute: function () {
            _export('QueryViewer', QueryViewer = (_dec = inject(QueryViewerService, I2B2Service, QueryViewerModel), _dec(_class = function () {
                function QueryViewer(service, i2b2Svc, model) {
                    var _this = this;

                    _classCallCheck(this, QueryViewer);

                    this.screenIndex = 0;
                    this.showCircles = false;
                    this.showLoader = true;
                    this.runningQuery = null;
                    this.service = service;
                    this.vertStyle = 'v-min';
                    this.scrollRatio = 0;

                    var parseResultToScreens = function parseResultToScreens(result) {
                        return _this.service.getScreens(result.adapters, result.queryResults);
                    };
                    var setVM = function setVM(screens) {
                        _this.showLoader = false;
                        _this.runningQuery = null;
                        _this.screens = screens;
                        _this.showCircles = _this.screens.length > 1;
                        model.screens = screens;
                        model.isLoaded = true;
                    };
                    var refresh = function refresh() {
                        return _this.service.fetchPreviousQueries().then(parseResultToScreens).then(setVM).catch(function (error) {
                            return console.log(error);
                        });
                    };

                    var addQuery = function addQuery(event, data) {
                        return _this.runningQuery = data[0].name;
                    };
                    var init = function init() {
                        return model.isLoaded ? setVM(model.screens) : refresh();
                    };

                    var isMinimized = function isMinimized(e) {
                        return e.action !== 'ADD';
                    };
                    var setVertStyle = function setVertStyle(a, b) {
                        return _this.vertStyle = b.find(isMinimized) ? 'v-min' : 'v-full';
                    };
                    i2b2Svc.onResize(setVertStyle);
                    i2b2Svc.onHistory(refresh);
                    i2b2Svc.onQuery(addQuery);
                    init();
                }

                QueryViewer.prototype.getContext = function getContext(event, result) {
                    return {
                        x: event.pageX,
                        y: event.pageY,
                        id: result.id,
                        class: 'show'
                    };
                };

                QueryViewer.prototype.onScroll = function onScroll(e) {
                    this.scrollRatio = e.target.clientHeight + e.target.scrollTop + '/' + e.target.scrollHeight;
                };

                return QueryViewer;
            }()) || _class));

            _export('QueryViewer', QueryViewer);
        }
    };
});
//# sourceMappingURL=query-viewer.js.map
