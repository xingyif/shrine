System.register(['aurelia-framework', 'views/query-viewer/query-viewer.service', 'common/i2b2.service.js'], function (_export, _context) {
    "use strict";

    var inject, computedFrom, QueryViewerService, I2B2Service, _dec, _class, QueryViewer;

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
        }],
        execute: function () {
            _export('QueryViewer', QueryViewer = (_dec = inject(QueryViewerService, I2B2Service), _dec(_class = function QueryViewer(service, i2b2Svc) {
                var _this = this;

                _classCallCheck(this, QueryViewer);

                this.screenIndex = 0;
                this.showCircles = false;
                this.service = service;
                this.vertStyle = 'v-min';

                var parseResultToScreens = function parseResultToScreens(result) {
                    return _this.service.getScreens(result.adapters, result.queryResults);
                };
                var setVM = function setVM(screens) {
                    _this.screens = screens;
                    _this.showCircles = _this.screens.length > 1;
                };
                var refresh = function refresh() {
                    return _this.service.fetchPreviousQueries().then(parseResultToScreens).then(setVM).catch(function (error) {
                        return console.log(error);
                    });
                };

                var isMinimized = function isMinimized(e) {
                    return e.action !== 'ADD';
                };
                i2b2Svc.onResize(function (a, b) {
                    return _this.vertStyle = b.find(isMinimized) ? 'v-min' : '';
                });
                i2b2Svc.onHistory(refresh);

                refresh();
            }) || _class));

            _export('QueryViewer', QueryViewer);
        }
    };
});
//# sourceMappingURL=query-viewer.js.map
