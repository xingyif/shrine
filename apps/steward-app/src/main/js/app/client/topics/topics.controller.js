(function () {
    'use strict';

    angular
        .module('shrine.steward.topics')
        .controller('TopicsController', TopicsController);

    TopicsController.$inject = ['TopicsService', 'TopicsModel', '$uibModal', '$scope'];
    function TopicsController(TopicsService, TopicsModel, $uibModal, $scope) {

        // -- set up locals --//
        var topics = this;
        var model = TopicsModel;
        var service = TopicsService;
        var initialState = null;
        var modelUpdate = model.getStewardTopics;


        // -- todo: move to service ? --//
        // -- public -- //
        topics.sortData = {
            sortDirection: 'ascending',
            arrowClass: 'fa-caret-down',
            column: 'id',
            pageIndex: service.viewConfig.index, // -- current page number --//
            range: service.viewConfig.range, // -- range of paging numbers at bottom --//
            limit: service.viewConfig.limit, // -- number of results to show in table per page --//
            length: 0, // -- total number of topic results --//
            skip: 0, // -- number of results to skip --//
            state: null // -- pending, approved, rejected --//
        };

        topics.showStewardMenu = service.isSteward();
        topics.update = update;
        topics.dateFormatter = service.dateFormatter;
        topics.openTopic = openTopic;
        topics.createTopic = createTopic;
        topics.onPageSelected = onPageSelected;

        if (!topics.showStewardMenu) {
            modelUpdate = model.getResearcherTopics;
        }

        init();

        function setDefaultState() {

            // -- local vars -- //
            var sortData = topics.sortData;
            sortData.state = service.states.state1; // -- pending, approved, rejected --//
            sortData.sortDirection = 'ascending';
            sortData.arrowClass = 'fa-caret-down';
            sortData.column = 'id';
            sortData.pageIndex = service.viewConfig.index; // -- current page number --//
            sortData.range = service.viewConfig.range; // -- range of paging numbers at bottom --//
            sortData.limit = service.viewConfig.limit; // -- number of results to show in table per page --//
            sortData.length = 0; // -- total number of topic results --//
            sortData.skip = 0; // -- number of results to skip --//

            if (!topics.showStewardMenu) {
                service.state = 'all';
            }
        }

        function toggleSort(column) {

            var sortData = topics.sortData;

            //change direction if same column is clicked.
            if (sortData.column === column) {

                // -- todo, dislike nested ifs --//
                if (topics.sortData.sortDirection != 'ascending') {
                    topics.sortData.sortDirection = 'ascending';
                    topics.sortData.arrowClass = 'fa-caret-down'
                }
                else {
                    topics.sortData.sortDirection = 'descending';
                    topics.sortData.arrowClass = 'fa-caret-up';
                }
            }

            //default is descending.
            else {
                topics.sortData.sortDirection = 'descending';
                topics.sortData.arrowClass = 'fa-caret-up';
                sortData.column = column;
            }
        }

        /*
         * Handler for when pagination page is changed.
         */
        function onPageSelected() {
            var mult;
            var sortData = topics.sortData;

            mult = (sortData.pageIndex > 0) ? sortData.pageIndex - 1 : 0;
            sortData.skip = sortData.limit * mult;
            refreshTopics(sortData.skip, sortData.limit);
        }

        function refreshTopics() {
            var sortData = topics.sortData;
            modelUpdate(sortData.skip, sortData.limit, sortData.state, sortData.column,
                sortData.sortDirection)
                .then(function (data) {
                    topics.topics = data.topics;
                    topics.length = data.totalCount;
                    sortData.length = data.totalCount;
                    sortData.totalPages = Math.ceil(sortData.length / sortData.limit);
                });
        }

        function startMenuWatch() {
            $scope.sortData = topics.sortData;
            $scope.$watch('sortData.state', function (newVal, oldVal) {
                if (oldVal !== newVal) {
                    refreshTopics();
                }
            });
        }

        function init() {
            setDefaultState();
            refreshTopics();
            startMenuWatch();
        }

        function update(column) {
            toggleSort(column);
            refreshTopics();
        }

        // -- todo create modal service instead of inlining code -- //
        function openTopic(topic) {

            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: './app/client/topics/directives/topic-detail.tpl.html',
                controller: TopicDetailController,
                controllerAs: 'detail',
                resolve: {
                    topic: function () {
                        return topic;
                    },
                    onClose: function () {
                        return refreshTopics;
                    }
                }
            });
        }

        function createTopic() {
            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: './app/client/topics/directives/new-topic.tpl.html',
                controller: NewTopicController,
                controllerAs: 'newTopic',
                resolve: {
                    onClose: function () {
                        return refreshTopics;
                    }
                },
            });
        }
    }

    // -- todo: should be in its own file. -- //
    NewTopicController.$inject = ['$uibModalInstance', 'onClose', 'TopicsModel'];
    function NewTopicController($uibModalInstance, onClose, TopicsModel) {

        var newTopic = this;
        var model = TopicsModel;
        newTopic.name = '';
        newTopic.description = '';

        // -- public --//
        newTopic.ok = ok;
        newTopic.cancel = cancel;

        // -- private -- //
        function ok() {
            var name = newTopic.name;
            var description = newTopic.description;

            makeRequest()
                .then(finish);
        }

        function makeRequest() {
            return model.requestNewTopic({
                'name': newTopic.name,
                'description': newTopic.description
            });
        }

        function finish() {
            onClose();
            $uibModalInstance.close();
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }
    }

    // -- ditto...own file --//
    TopicDetailController.$inject = ['$scope', '$uibModalInstance', 'topic', 'onClose', 'TopicsService', 'TopicsModel'];
    function TopicDetailController($scope, $uibModalInstance, topic, onClose, TopicsService, TopicsModel) {
        var detail = this;
        var service = TopicsService;
        var model = TopicsModel;
        var isSteward = service.isSteward();
        var dateFormatter = service.dateFormatter;
        var topicDescription = topic.description;
        var loadedState = topic.state;

        detail.topic = topic;
        detail.topicState = loadedState;
        detail.tabState = 'description';
        detail.isSteward = isSteward;
        detail.dateFormatter = dateFormatter;
        detail.topicName = topic.name;
        detail.topicDescription = topic.description;


        // -- public methods --//
        detail.cancel = cancel;
        detail.canViewHistory = canViewHistory;
        detail.showSteward = isSteward;
        detail.isEditable = isEditable;
        detail.setState = setState;
        detail.update = update;

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

        function isEditable() {
            return isSteward || topic.state === 'Pending';
        }

        function setState(state) {
            if (isEditable()) {
                detail.tabState = state;
            }
        }

        function finish() {
            onClose();
            $uibModalInstance.dismiss('cancel');
        }

        // -- private -- //
        function stewardUpdate() {
            topic.state = detail.topicState;

            switch (topic.state) {
                case 'Approved':
                    model.approveTopic(topic.id)
                        .then(finish);
                    break;
                case 'Rejected':
                    model.rejectTopic(topic.id)
                        .then(finish);
                    break;
                default:
                    $uibModalInstance.close($scope.topic);
            }
        }

        function researcherUpdate() {
            model.updateTopic({
                name: detail.topicName,
                description: detail.topicDescription,
                id: detail.topic.id
            })
                .then(finish);
        }

        function update() {
            if (isSteward) {
                stewardUpdate();
            }
            else {
                researcherUpdate();
            }
        }

        function canViewHistory() {

            var stewardCanViewApprovedAndRejected = isSteward && $scope.loadedState !== 'Pending';
            var researcherCanViewOnlyApproved = loadedState === 'Approved';
            var ifEditingDenyAll = detail.tabState === 'edit';

            if (ifEditingDenyAll) {
                return false;
            }

            return stewardCanViewApprovedAndRejected || researcherCanViewOnlyApproved;
        }
    }
})();