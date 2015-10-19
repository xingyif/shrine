'use strict';
/**
 * @ngdoc function
 * @name sbAdminApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the sbAdminApp
 * https://scotch.io/tutorials/sort-and-filter-a-table-using-angular
 */
angular.module('stewardApp')
        .controller("TopicDetailCtrl", function ($scope, $modalInstance, topic, $app, Role2TopicDetailMdl) {

        $scope.roles        = $app.globals.UserRoles;
        $scope.userRole     = $app.globals.currentUser.roles[0];
        $scope.topic        = topic;
        $scope.tabState     = 'description';
        $scope.formatDate   = $app.utils.utcToMMDDYYYY;


        $scope.ok = function (id) {
            if ($scope.topic.state === "Pending") {
                $modalInstance.close($scope.topic);
                return;
            }

            (($scope.topic.state == "Approved") ?
                Role2TopicDetailMdl.approveTopic(id) :
                Role2TopicDetailMdl.rejectTopic(id))
                .then(function (result) {
                    $scope.modalCallback();
                    $modalInstance.close(result);
                });
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    })
    .controller("NewTopicCtrl", function ($scope, $modalInstance, model, refreshTopics) {

        $scope.newTopic = {name: "", description: ""};
        $scope.ok = function () {
            var name        = $scope.newTopic.name,
                description = $scope.newTopic.description;
            model.requestNewTopic({"name": name, "description": description})
                .then(function () {
                    refreshTopics();
                    $modalInstance.close();
                });
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    })
    .controller('TopicsCtrl', function ($scope, $position, $app, TopicsModelFactory, $modal) {

        //private vars
        var roles = $app.globals.UserRoles,
            user  = $app.globals.currentUser,
            utils = $app.utils,
            scope = $scope,
            ModelFactory = TopicsModelFactory,
            role, model, initState;

        //determine role of user.
        role        = (utils.hasAccess(user, [roles.ROLE2])) ? roles.ROLE2 : roles.ROLE1;
        initState   = (role === roles.ROLE2) ? $app.globals.States.STATE1 : "ALL";
        model       = ModelFactory.getInstance(role);

        $scope.model = model;

        $scope.roles      = roles;
        $scope.userRole   = role;
        $scope.formatDate = $app.utils.utcToMMDDYYYY;

        $scope.states = $app.globals.States;
        $scope.topics = [];
        $scope.filterBy = 'topicName';
        $scope.sort = {
            currentColumn: 'changeDate',
            descending: true
        };
        $scope.selectedTab = initState;

        /**
         * Parent scope data referenced by children.
         * @type {{topics: Array, name: string, description: string, sort: {column: string, descending: boolean}}}
         */
        $scope.data = {
            name:        'name',
            description: 'description'
        };

        //set pagination values.
        $scope.range        = $app.globals.ViewConfig.RANGE;//range of paging numbers at bottom
        $scope.length       = 0;                            //total number of results
        $scope.pageIndex    = $app.globals.ViewConfig.INDEX;//current page
        $scope.limit        = $app.globals.ViewConfig.LIMIT;//number of results to show in table at per page.
        $scope.skip         = 0;                            //number of results to skip.

        /*
         * Handler for when pagination page is changed.
         */
        $scope.onPageSelected = function () {
            var mult;
            mult             = ($scope.pageIndex > 0) ? $scope.pageIndex - 1 : 0;
            $scope.skip      = $scope.limit * mult;
            $scope.refreshTopics($scope.skip, $scope.limit);
        };

        $scope.setStateAndRefresh = function (state) {
            $scope.state =  state;

            if ($scope.sort) {
                $scope.sort.currentColumn = 'changeDate';
                $scope.sort.descending    = true;
            }

            $scope.refreshTopics();
        };

        $scope.refreshTopics = function () {

            var state, sortBy, sortDirection;

            if ($scope.state !== "ALL") {
                state = $scope.state;
            }

            if ($scope.sort && $scope.sort.currentColumn !== "") {
                sortBy          = $scope.sort.currentColumn;
                sortDirection   = ($scope.sort.descending) ? "descending" : "ascending";
            }
            $scope.model.getTopics($scope.skip, $scope.limit, state, sortBy, sortDirection)
                .then(function (result) {
                    scope.topics    = result.topics;
                    $scope.length   = result.totalCount;
                });
        };

        $scope.getTitle = function (state) {
            var title = " Query Topics";

            return (role === roles.ROLE2) ?
                ($scope.state + title) : title;
        };

        //method wrapper for scope resolution in modal context.
        function requestNewTopic(topic) {
            if (model.requestNewTopic !== undefined) {
                return model.requestNewTopic(topic);
            }
        }

        //method wrapper for scope resolution in modal context.
        function refreshTopics() {
            return $scope.refreshTopics();
        }

        $scope.createTopic = function () {
            var model = $scope.model;
            var modalInstance = $modal.open({
                animation: true,
                templateUrl: 'src/app/dashboard/topics/new-topic/new-topic.tpl.html',
                controller: function ($scope, $modalInstance) {

                    $scope.newTopic = {name: "", description: ""};
                    $scope.ok = function () {
                        var name        = $scope.newTopic.name,
                            description = $scope.newTopic.description;
                        requestNewTopic({"name": name, "description": description})
                            .then(function () {
                                refreshTopics();
                                $modalInstance.close();
                            });
                    };

                    $scope.cancel = function () {
                        $modalInstance.dismiss('cancel');
                    };
                },
                //size: undefined,
                resolve: {
                    model:    model,
                    refreshTopics: $scope.refreshTopics
                }
            });
        };

        $scope.open = function (topic) {
            var modalInstance = $modal.open({
                animation: true,
                templateUrl: 'src/app/dashboard/topics/topic-table/topic-detail.tpl.html',
                controller: function ($scope, $modalInstance, topic, $app, Role2TopicDetailMdl, Role1TopicDetailMdl){
                    $scope.roles        = $app.globals.UserRoles;
                    $scope.userRole     = $app.globals.currentUser.roles[0];
                    $scope.topic        = topic;
                    $scope.tabState     = 'description';
                    $scope.formatDate   = $app.utils.utcToMMDDYYYY;


                    $scope.ok = function (id) {
                        if ($scope.topic.state === "Pending") {
                            $modalInstance.close($scope.topic);
                            return;
                        }

                        (($scope.topic.state == "Approved") ?
                            Role2TopicDetailMdl.approveTopic(id) :
                            Role2TopicDetailMdl.rejectTopic(id))
                                .then(function (result) {
                                    refreshTopics();
                                    $modalInstance.close(result);
                                });
                    };

                    $scope.cancel = function () {
                        $modalInstance.dismiss('cancel');
                    };

                    $scope.isEditable =  function () {
                        return ($app.globals.currentUser.roles[0] === $scope.roles.ROLE2) || ($scope.topic.state === "Pending");
                    };

                    $scope.setState = function (state) {
                        if ($scope.isEditable() === true) {
                            $scope.tabState = state;
                        }
                    };

                    $scope.update = function (id, name, description) {
                        Role1TopicDetailMdl.updateTopic(id, name, description)
                            .then( function (result) {
                                refreshTopics();
                                $modalInstance.close(result);
                            });
                    };

                    $scope.canViewHistory = function () {
                        var canView =  ($app.globals.currentUser.roles[0] === $scope.roles.ROLE2 && $scope.topic.state !== "Pending") || ($scope.topic.state === "Approved");
                        return canView;
                    };
                },
                //size: undefined,
                resolve: {
                    topic: function () {
                        return topic;
                    }
                }
            });
        };

        $scope.setStateAndRefresh(initState);

    })
    .directive("topicTable", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/topics/topic-table/topic-table.tpl.html",
            replace: true
        };
    })
    .directive("role1Description", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/topics/topic-table/topic-detail/role1-description.tpl.html",
            replace: true
        };
    })
    .directive("role2Description", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/topics/topic-table/topic-detail/role2-description.tpl.html",
            replace: true
        };
    })
    .directive("role1Edit", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/topics/topic-table/topic-detail/role1-edit.tpl.html",
            replace: true
        };
    })
    .directive("role2Edit", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/topics/topic-table/topic-detail/role2-edit.tpl.html",
            replace: true
        };
    });


