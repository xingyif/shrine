System.register(['aurelia-framework', 'aurelia-event-aggregator', 'common/queries.model', './scroll.service', 'common/shrine.messages'], function (_export, _context) {
    "use strict";

    var inject, EventAggregator, QueriesModel, ScrollService, notifications, commands, _dec, _class, QueryViewer;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_aureliaEventAggregator) {
            EventAggregator = _aureliaEventAggregator.EventAggregator;
        }, function (_commonQueriesModel) {
            QueriesModel = _commonQueriesModel.QueriesModel;
        }, function (_scrollService) {
            ScrollService = _scrollService.ScrollService;
        }, function (_commonShrineMessages) {
            notifications = _commonShrineMessages.notifications;
            commands = _commonShrineMessages.commands;
        }],
        execute: function () {
            _export('QueryViewer', QueryViewer = (_dec = inject(EventAggregator, QueriesModel, notifications, commands), _dec(_class = function () {
                function QueryViewer(evtAgg, queries, notifications, commands) {
                    var _this = this;

                    _classCallCheck(this, QueryViewer);

                    QueryViewer.prototype.init = function () {
                        _this.pageIndex = 0;
                        _this.showCircles = false;
                        _this.showLoader = true;
                        _this.vertStyle = 'v-min';
                        _this.runningQueryName = null;
                    };
                    this.init();

                    QueryViewer.prototype.setToPage = function (i) {
                        _this.pageIndex = i;
                        _this.page = _this.pages[_this.pageIndex];
                    };
                    var scrolledToBottom = function scrolledToBottom(e) {
                        return ScrollService.scrollRatio(e).value === 1;
                    };
                    QueryViewer.prototype.onScroll = function (e) {
                        if (scrolledToBottom(e) && !_this.loadingInfiniteScroll && queries.moreToLoad()) {
                            _this.loadingInfiniteScroll = true;
                            queries.load();
                        }
                    };

                    QueryViewer.prototype.publishError = function (e) {
                        return evtAgg.publish(commands.i2b2.showError, e);
                    };
                    QueryViewer.prototype.getContext = function (e, r) {
                        return { x: e.pageX, y: e.pageY, id: r.id, class: 'show' };
                    };

                    evtAgg.subscribe(notifications.i2b2.historyRefreshed, function () {
                        return queries.load();
                    });
                    evtAgg.subscribe(notifications.i2b2.tabMax, function () {
                        return _this.vertStyle = 'v-full';
                    });
                    evtAgg.subscribe(notifications.i2b2.tabMin, function () {
                        return _this.vertStyle = 'v-min';
                    });
                    evtAgg.subscribe(notifications.i2b2.queryStarted, function (n) {
                        return _this.runningQueryName = n;
                    });
                    evtAgg.subscribe(notifications.shrine.queriesReceived, function (d) {
                        _this.pages = d;
                        _this.showCircles = _this.pages.length > 1;
                        _this.page = _this.pages[0];
                        _this.runningQueryName = null;
                        _this.loadingInfiniteScroll = false;
                        _this.showLoader = false;
                    });
                }

                QueryViewer.prototype.testCall = function testCall($event) {
                    $event.stopPropagation();
                    var index = event.detail.index;
                    this.page = this.pages[index];
                };

                return QueryViewer;
            }()) || _class));

            _export('QueryViewer', QueryViewer);
        }
    };
});
//# sourceMappingURL=query-viewer.js.map
