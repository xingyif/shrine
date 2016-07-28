(function () {
    'use strict';

    angular
        .module('shrine.steward.topics')
        .controller('TopicsController', TopicsController);

    TopicsController.$inject = ['TopicsService', 'TopicsModel', '$uibModal'];
    function TopicsController(TopicsService, TopicsModel, $uibModal) {

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
            // -- todo?:  refreshTopics(topics.sortData.skip, topics.sortData.limit);
        };

        function refreshTopics() {
            var sortData = topics.sortData;
            modelUpdate(sortData.skip, sortData.limit, sortData.state, sortData.column,
                sortData.sortDirection)
                .then(function (data) {
                    topics.topics = data.topics;
                    topics.length = data.totalCount;
                });
        }

        function init() {
            setDefaultState();
            refreshTopics();
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
                    }
                }
            });
        }

        function createTopic() {
            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: './app/client/topics/directives/new-topic.tpl.html',
                resolve: {
                    onClose: refreshTopics
                },
                controller: NewTopicController,
                controllerAs: 'newTopic'
            });
        }
    }

    // -- todo: should be in its own file. -- //
    function NewTopicController($uibModalInstance, onClose) {

        var newTopic = this;
        newTopic.name = '';
        newTopic.description = '';

        // -- public --//
        newTopic.ok = ok;
        newTopic.cancel = cancel;

        // -- private -- //
        function ok() {

            var name = newTopic.name;
            var description = newTopic.description;

            //     requestNewTopic({ "name": name, "description": description })
            //         .then(function () {
            //             refreshTopics();
            //             $modalInstance.close();
            //         });
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }
    }



    // -- ditto...own file --//
    TopicDetailController.$inject = ['$scope', '$uibModalInstance', 'topic', 'TopicsService', 'TopicsModel'];
    function TopicDetailController($scope, $uibModalInstance, topic, TopicsService, TopicsModel) {
        var detail = this;
        var service = TopicsService;
        var isSteward = service.isSteward();
        var dateFormatter = service.dateFormatter;
        var loadedState = topic.state;
        var topicDescription = topic.description;

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

        // -- private -- //
        function ok(id) {
            topic.state = detail.topicState;

            if (topic.state === 'Pending') {
                $uibModalInstance.close($scope.topic);
                return;
            }
            else if (topic.state == 'Approved') {
                // Role2TopicDetailMdl.approveTopic(id) :
                // Role2TopicDetailMdl.rejectTopic(id))
                // .then(function (result) {
                //     refreshTopics();
                //     $uibModalInstance.close(result);
                // });
            }
        }

        function cancel() {
            $uibModalInstance.dismiss('cancel');
        }

        function isEditable() {
            return isSteward || topic.state == 'Pending';
        }

        function setState(state) {
            if (isEditable()) {
                detail.tabState = state;
            }
        }

        function update(id, name, description) {
            // Role1TopicDetailMdl.updateTopic(id, name, description)
            //     .then(function (result) {
            //         refreshTopics();
            //         $uibModalInstance.close(result);
            //     });
        }


        function canViewHistory() {
            var onlyStewardCanView = (isSteward && loadedState !== 'Pending');
            var allCanView = (loadedState === 'Approved' && detail.tabState === 'edit');

            return onlyStewardCanView || allCanView;
        }
    }
})();