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
            arrowClass: 'fa-caret-up',
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
            sortData.arrowClass = 'fa-caret-up';
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
             var mult;
            var sortData = history.sortData;
           
            mult = (sortData.pageIndex > 0) ? sortData.pageIndex - 1 : 0;
            sortData.skip = sortData.limit * mult;
            refreshHistory(sortData.skip, sortData.limit);
        }


        function setViewData(result) {
            var sortData = history.sortData;
            history.queries = result.queryRecords;
            history.length = result.totalCount;
            sortData.length = result.totalCount;
            sortData.totalPages = Math.ceil(sortData.length / sortData.limit);
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
                modelMethod = model.getStewardHistory;
            } else {
                modelMethod = model.getResearcherHistory;
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

