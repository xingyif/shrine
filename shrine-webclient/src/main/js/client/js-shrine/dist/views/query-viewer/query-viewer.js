System.register(['aurelia-framework', 'views/query-viewer/query-viewer.service', 'common/i2b2.service.js', 'common/tabs.model', './query-viewer.model', './scroll.service', './query-viewer.config'], function (_export, _context) {
    "use strict";

    var inject, computedFrom, QueryViewerService, I2B2Service, TabsModel, QueryViewerModel, ScrollService, QueryViewerConfig, _dec, _class, QueryViewer;

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
        }, function (_commonTabsModel) {
            TabsModel = _commonTabsModel.TabsModel;
        }, function (_queryViewerModel) {
            QueryViewerModel = _queryViewerModel.QueryViewerModel;
        }, function (_scrollService) {
            ScrollService = _scrollService.ScrollService;
        }, function (_queryViewerConfig) {
            QueryViewerConfig = _queryViewerConfig.QueryViewerConfig;
        }],
        execute: function () {
            _export('QueryViewer', QueryViewer = (_dec = inject(QueryViewerService, I2B2Service, QueryViewerModel, TabsModel), _dec(_class = function () {
                function QueryViewer(service, i2b2Svc, model, tabs) {
                    var _this = this;

                    _classCallCheck(this, QueryViewer);

                    this.screenIndex = 0;
                    this.showCircles = false;
                    this.showLoader = true;
                    this.runningQuery = null;
                    this.service = service;
                    this.vertStyle = tabs.mode();
                    this.scrollRatio = 0;

                    var parseResultToScreens = function parseResultToScreens(result) {
                        model.totalQueries = result.rowCount;
                        model.loadedCount = result.queryResults.length;
                        return _this.service.getScreens(result.adapters, result.queryResults);
                    };
                    var setVM = function setVM(screens) {
                        _this.showLoader = false;
                        _this.runningQuery = null;
                        _this.screens = screens;
                        _this.showCircles = _this.screens.length > 1;
                        model.screens = screens;
                        model.processing = false;
                    };

                    var refresh = function refresh() {
                        return _this.service.fetchPreviousQueries(model.loadedCount + QueryViewerConfig.maxQueriesPerScroll).then(parseResultToScreens).then(setVM).catch(function (error) {
                            return console.log(error);
                        });
                    };

                    var addQuery = function addQuery(event, data) {
                        return _this.runningQuery = data[0].name;
                    };
                    var init = function init() {
                        return model.hasData ? setVM(model.screens) : refresh();
                    };
                    var loadMoreQueries = function loadMoreQueries(e) {
                        return ScrollService.scrollRatio(e).value === 1 && model.moreToLoad && !model.processing;
                    };

                    this.onScroll = function (e) {
                        if (loadMoreQueries(e)) {
                            refresh();
                            model.processing = true;
                        }
                    };

                    var isMinimized = function isMinimized(e) {
                        return e.action !== 'ADD';
                    };
                    var setVertStyle = function setVertStyle(a, b) {
                        return _this.vertStyle = b.find(isMinimized) ? 'v-min' : 'v-full';
                    };
                    this.errorDetail = i2b2Svc.errorDetail;
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

                return QueryViewer;
            }()) || _class));

            _export('QueryViewer', QueryViewer);
        }
    };
});
//# sourceMappingURL=query-viewer.js.map
