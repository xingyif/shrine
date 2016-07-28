(function () {
    angular.module('shrine.steward')
        .directive('queryHistory', QueryHistoryDirective);

    function QueryHistoryDirective() {
        var history = {
            restrict: 'E',
            templateUrl: './app/client/directives/history.tpl.html',
            controller: QueryHistoryController,
            controllerAs: 'history',
            scope: {
                topic: '='
            }
        };

        return history;
    }

    QueryHistoryController.$inject = ['$scope', 'HistoryModel', 'HistoryService', '$uibModal'];
    function QueryHistoryController($scope, HistoryModel, HistoryService, $uibModal) {

        // -- local -- //
        var history = this;
        var topic = $scope.topic;
        var model = HistoryModel;
        var service = HistoryService;
        var dateFormatter = service.dateFormatter;
        var xmlFormatter = service.xmlFormatter;
        var isSteward = service.isSteward();

        // -- public -- //
        history.topic = topic;
        history.queries = [];
        history.dateFormatter = dateFormatter;
        history.xmlFormatter = xmlFormatter;
        history.sortData = {
            sortDirection: 'ascending',
            arrowClass: 'fa-caret-down',
            column: 'date',
            pageIndex: service.viewConfig.index, // -- current page number --//
            range: service.viewConfig.range, // -- range of paging numbers at bottom --//
            limit: service.viewConfig.limit, // -- number of results to show in table per page --//
            length: 0, // -- total number of topic results --//
            skip: 0 // -- number of results to skip --//
        };

        // -- public -- //
        history.sort = sort;
        history.open = openQuery;

        init();

        // -- private -- //
        function init() {
            setDefaultState();
            refreshHistory();
        }

        function sort(column) {
            toggleSort(column);
            refreshHistory();
        }

        function getTopicTitle(topic) {
            return (topic) ? topic.name + ' Query History' : 'Query History';
        }

        function getQueryTitle(queryXml) {
            var queryAsJson = xmlFormatter(queryXml);
            return queryAsJson.queryDefinition.name;
        }

        function setDefaultState() {

            // -- local vars -- //
            var sortData = history.sortData;
            sortData.sortDirection = 'ascending';
            sortData.arrowClass = 'fa-caret-down';
            sortData.column = 'date';
            sortData.pageIndex = service.viewConfig.index; // -- current page number --//
            sortData.range = service.viewConfig.range; // -- range of paging numbers at bottom --//
            sortData.limit = service.viewConfig.limit; // -- number of results to show in table per page --//
            sortData.length = 0; // -- total number of topic results --//
            sortData.skip = 0; // -- number of results to skip --//
        }

        function toggleSort(column) {

            var sortData = history.sortData;

            //change direction if same column is clicked.
            if (sortData.column === column) {

                // -- todo, dislike nested ifs --//
                if (history.sortData.sortDirection != 'ascending') {
                    history.sortData.sortDirection = 'ascending';
                    history.sortData.arrowClass = 'fa-caret-down';
                }
                else {
                    history.sortData.sortDirection = 'descending';
                    history.sortData.arrowClass = 'fa-caret-up';
                }
            }

            //default is descending.
            else {
                history.sortData.sortDirection = 'descending';
                history.sortData.arrowClass = 'fa-caret-up';
                sortData.column = column;
            }
        }

        /*
         * Handler for when pagination page is changed.
         */
        function onPageSelected() {
            var sortData = history.sortData;
            var mult;
            mult = (sortData.pageIndex > 0) ? sortData.pageIndex - 1 : 0;
            sortData.skip = sortData.limit * mult;
            ///todo?   $scope.refreshHistory($scope.skip, $scope.limit);
        }


        function setViewData(result) {
            history.queries = result.queryRecords;
            history.length = result.totalCount;
        }

        /*
         * Refresh Query History -  get a range of query history results.
         * @param: skip     - number of result to skip
         * @param: limit    - number of results.
         */
        function refreshHistory() {

            var sortData = history.sortData;
            var topicId = history.topic ? history.topic.id : undefined;
            var modelMethod;

            if (isSteward) {
                modelMethod = model.getResearcherHistory;
            } else {
                modelMethod = model.getStewardHistory;
            }

            modelMethod(sortData.skip, sortData.limit, sortData.column,
                sortData.sortDirection, topicId)
                .then(setViewData);
        }


        // -- todo create modal service instead of inlining code -- //
        function openQuery(query) {

            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: './app/client/directives/query-detail.tpl.html',
                controller: QueryDetailController,
                controllerAs: 'detail',
                resolve: {
                    query: function () {
                        return query;
                    }
                }
            });
        }
    }

    // -- todo: should be in own file query-detail.controller.js -- //
    QueryDetailController.$inject = ['$uibModalInstance', 'query', 'HistoryService'];
    function QueryDetailController($uibModalInstance, query, HistoryService) {

        // -- setup --//
        var service = HistoryService;
        var detail = this;
        detail.query = query;
        detail.prettyName = service.prettifyName(query.name);
        detail.prettyDetail = service.prettifyContents(query.queryContents);
        detail.ok = close;
        detail.cancel = close;

        function close() {
            $uibModalInstance.dismiss('cancel');
        }
    }
})();


/**
* Query History
* @author   Ben Carmen
* @date     04/04/2015
* Edit Log:
*    @todo: bdc -- 04-04-15 -- consider making steward role default a utils. method.
*    @todo: bdc -- 04-06-15 -- change $scope.data.queries to $scope.queries.
*/
/*angular.module('stewardApp')
    .directive('history', function (HistoryMdl, $app, $modal) {
        return {
            restrict: 'E',
            scope: {
                topic: "="
            },
            templateUrl: 'app/client/dashboard/history/history.tpl.html',
            controller: function ($scope) {

                //private
                var roles = $app.globals.UserRoles,
                    user = $app.globals.currentUser,
                    utils = $app.utils,
                    model, role;

                //steward by default.
                role = (utils.hasAccess(user, [roles.ROLE2])) ? roles.ROLE2 : roles.ROLE1;
                model = HistoryMdl.getInstance(role);

                $scope.queries = [];
                $scope.formatDate = $app.utils.utcToMMDDYYYY;
                $scope.sort = {
                    currentColumn: 'date',
                    descending: false
                }

                $scope.getTopicTitle = function (topic) {
                    return (topic !== undefined) ? topic.name + ' Query History' : 'Query History';
                };

                $scope.getQueryTitle = function (queryXml) {
                    var queryAsJson = utils.xmlToJson(queryXml);
                    return queryAsJson.queryDefinition.name;
                };

                /**
                 * Modal Configuration for creating a new topic.
                 * @type {{templateUrl: string, controller: Function}}
                 
                $scope.modalConfig = {
                    templateUrl: 'app/client/history/query-detail/query-detail.tpl.html',
                    controller: function ($scope, $modalInstance, modalData) {

                        $scope.query = modalData;

                        $scope.prettify = function (queryData) {

                            var array = queryData.split('<'),
                                tab = '\t',
                                enter = '\n';

                            //traverse array

                            var ret = array.join(enter + tab + '<');

                            return ret;
                        };

                        $scope.ok = function () {
                            $modalInstance.dismiss('cancel');
                        };

                        $scope.queryContents = $scope.prettify($scope.query.queryContents);
                    }
                };

                //set pagination values.
                $scope.range = $app.globals.ViewConfig.RANGE;//range of paging numbers at bottom
                $scope.length = 0;                            //total number of results
                $scope.pageIndex = $app.globals.ViewConfig.INDEX;//current page
                $scope.limit = $app.globals.ViewConfig.LIMIT;//number of results to show in table at per page.
                $scope.skip = 0;                            //number of results to skip.

                /*
                 * Refresh Query History -  get a range of query history results.
                 * @param: skip     - number of result to skip
                 * @param: limit    - number of results.
                 
                $scope.refreshHistory = function () {

                    var topicId = ($scope.topic !== undefined) ? $scope.topic.id : undefined,
                        sortBy, sortDirection;

                    if ($scope.sort && $scope.sort.currentColumn !== "") {
                        sortBy = $scope.sort.currentColumn;
                        sortDirection = ($scope.sort.descending) ? "descending" : "ascending";
                    }

                    $scope.queryRecords = model.getHistory($scope.skip, $scope.limit, sortBy, sortDirection, topicId)
                        .then(function (result) {
                            $scope.queries = result.queryRecords;
                            $scope.length = result.totalCount;
                        });
                };

                /*
                 * Handler for when pagination page is changed.
                 
                $scope.onPageSelected = function () {
                    var mult;
                    mult = ($scope.pageIndex > 0) ? $scope.pageIndex - 1 : 0;
                    $scope.skip = $scope.limit * mult;
                    $scope.refreshHistory($scope.skip, $scope.limit);
                };

                /**
                 *
                 * @param columnId
                 
                $scope.setSort = function (columnId) {

                    //change direction if same column is clicked.
                    if ($scope.sort.currentColumn == columnId) {
                        $scope.sort.descending = !$scope.sort.descending;
                        return;
                    }

                    //start out the sorting as ascending.
                    $scope.sort.descending = false;
                    $scope.sort.currentColumn = columnId;
                }


                /**
                 *
                 
                $scope.showQuery = function (query) {

                    var model = $scope.model;
                    var modalInstance = $modal.open({
                        animation: true,
                        templateUrl: 'app/client/dashboard/history/history-table/query-detail.tpl.html',
                        controller: function ($scope, $modalInstance, query) {

                            $scope.displayName = (query.name.length > 50) ? (query.name.substring(0, 50) + '...') : query.name;
                            $scope.query = query;

                            $scope.prettify = function (queryData) {

                                var array = queryData.split('<'),
                                    tab = '\t',
                                    enter = '\n';

                                var ret = array.join(enter + tab + '<');

                                return ret;
                            };

                            $scope.ok = function () {
                                $modalInstance.dismiss('cancel');
                            };

                            $scope.cancel = function () {
                                $modalInstance.dismiss('cancel');
                            };

                            $scope.queryContents = $scope.prettify($scope.query.queryContents);
                        },
                        //size: undefined,
                        resolve: {
                            query: function () {
                                return query;
                            }
                        }
                    });
                };

                $scope.refreshHistory();
            }
        };
    })
    .directive("historyTable", function () {
        return {
            restrict: "E",
            templateUrl: "app/client/dashboard/history/history-table/history-table.tpl.html",
            replace: true
        };
    });*/