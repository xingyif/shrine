System.register(['aurelia-framework', 'views/query-viewer/query-viewer.service'], function (_export, _context) {
    "use strict";

    var inject, computedFrom, QueryViewerService, _dec, _class, QueryViewer;

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
        }],
        execute: function () {
            _export('QueryViewer', QueryViewer = (_dec = inject(QueryViewerService), _dec(_class = function QueryViewer(service) {
                var _this = this;

                _classCallCheck(this, QueryViewer);

                this.screenIndex = 0;
                this.showCircles = false;
                this.service = service;
                this.service.fetchPreviousQueries().then(function (result) {
                    var queries = result.queryResults;
                    var nodes = result.adapters;
                    return service.getScreens(nodes, queries);
                }).then(function (screens) {
                    _this.screens = screens;
                    _this.showCircles = _this.screens.length > 1;
                }).catch(function (error) {
                    return console.log(error);
                });
            }) || _class));

            _export('QueryViewer', QueryViewer);
        }
    };
});
//# sourceMappingURL=query-viewer.js.map
