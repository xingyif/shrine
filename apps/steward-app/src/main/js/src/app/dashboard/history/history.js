/**
* Query History
* @author   Ben Carmen
* @date     04/04/2015
* Edit Log:
*    @todo: bdc -- 04-04-15 -- consider making steward role default a utils. method.
*    @todo: bdc -- 04-06-15 -- change $scope.data.queries to $scope.queries.
*/
angular.module('stewardApp')
    .directive('queryHistory', function (HistoryMdl, $app, $modal) {
        return {
            restrict: 'E',
            scope: {
                topic: "="
            },
            templateUrl: 'src/app/dashboard/history/history.tpl.html',
            controller: function ($scope) {

                //private
                var roles = $app.globals.UserRoles,
                    user  = $app.globals.currentUser,
                    utils = $app.utils,
                    model, role;

                //steward by default.
                role    = (utils.hasAccess(user, [roles.ROLE2])) ? roles.ROLE2 : roles.ROLE1;
                model   = HistoryMdl.getInstance(role);

                $scope.queries      = [];
                $scope.formatDate   = $app.utils.utcToMMDDYYYY;

                $scope.getTopicTitle = function (topic) {
                    return (topic !== undefined)? topic.name + ' Query History' : 'Query History';
                };

                $scope.getQueryTitle = function (queryXml) {
                    var queryAsJson = utils.xmlToJson(queryXml);
                    return queryAsJson.queryDefinition.name;
                };

                /**
                 * Modal Configuration for creating a new topic.
                 * @type {{templateUrl: string, controller: Function}}
                 */
                $scope.modalConfig = {
                    templateUrl: 'src/app/history/query-detail/query-detail.tpl.html',
                    controller: function ($scope, $modalInstance, modalData) {

                        $scope.query = modalData;

                        $scope.prettify = function (queryData) {

                            var array = queryData.split('<'),
                                tab   = '\t',
                                enter = '\n';

                            //traverse array

                            var ret =  array.join(enter + tab + '<');

                            return ret;
                        };

                        $scope.ok = function () {
                            $modalInstance.dismiss('cancel');
                        };

                        $scope.queryContents =  $scope.prettify($scope.query.queryContents);
                    }
                };

                //set pagination values.
                $scope.range        = $app.globals.ViewConfig.RANGE;//range of paging numbers at bottom
                $scope.length       = 0;                            //total number of results
                $scope.pageIndex    = $app.globals.ViewConfig.INDEX;//current page
                $scope.limit        = $app.globals.ViewConfig.LIMIT;//number of results to show in table at per page.
                $scope.skip         = 0;                            //number of results to skip.

                /*
                 * Refresh Query History -  get a range of query history results.
                 * @param: skip     - number of result to skip
                 * @param: limit    - number of results.
                 */
                $scope.refreshHistory = function () {

                    var topicId = ($scope.topic !== undefined) ? $scope.topic.id : undefined,
                        sortBy, sortDirection;

                    if ($scope.sort && $scope.sort.currentColumn !== "") {
                        sortBy          = $scope.sort.currentColumn;
                        sortDirection   = ($scope.sort.descending) ? "descending" : "ascending";
                    }
                    else {
                        sortBy          = 'date';
                        sortDirection   = 'ascending';
                    }

                    $scope.queryRecords = model.getHistory($scope.skip, $scope.limit, sortBy, sortDirection, topicId)
                        .then(function (result) {
                            $scope.queries = result.queryRecords;
                            $scope.length  = result.totalCount;
                        });
                };

                /**
                 * page Index is updated by paginator.
                 * @param data
                 */
                $scope.onPageSelected = function (data) {
                    var mult;
                    $scope.pageIndex = data.pageIndex;
                    mult             = ($scope.pageIndex > 0) ? $scope.pageIndex - 1 : 0;
                    $scope.skip      = $scope.limit * mult;
                    $scope.refreshHistory($scope.skip, $scope.limit);
                };


                /**
                 *
                 */
                $scope.showQuery = function (query) {

                    var model = $scope.model;
                    var modalInstance = $modal.open({
                        animation: true,
                        templateUrl: 'src/app/dashboard/history/history-table/query-detail.tpl.html',
                        controller: function ($scope, $modalInstance, query) {

                            $scope.query = query;

                            $scope.prettify = function (queryData) {

                                var array = queryData.split('<'),
                                    tab   = '\t',
                                    enter = '\n';

                                var ret =  array.join(enter + tab + '<');

                                return ret;
                            };

                            $scope.ok = function () {
                                $modalInstance.dismiss('cancel');
                            };

                            $scope.cancel = function () {
                                $modalInstance.dismiss('cancel');
                            };

                            $scope.queryContents =  $scope.prettify($scope.query.queryContents);
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
            templateUrl: "src/app/dashboard/history/history-table/history-table.tpl.html",
            replace: true
        };
    });