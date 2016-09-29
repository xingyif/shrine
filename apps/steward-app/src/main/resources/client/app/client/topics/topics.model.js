(function () {
    'use strict';

    angular
        .module('shrine.steward.topics')
        .factory('TopicsModel', TopicsModel);

    TopicsModel.$inject = ['$http', 'TopicsService'];
    function TopicsModel($http, TopicsService) {

        // -- local vars --//
        var topicsModel = this;
        var service = TopicsService;
        var urls = {
            stewardTopics: 'steward/topics',
            researcherTopics: 'researcher/topics',
            researcherRequestAccess: 'researcher/requestTopicAccess',
            researchEditTopic: 'researcher/editTopicRequest/',
            stewardApproveTopic: 'steward/approveTopic/topic/',
            stewardRejectTopic: 'steward/rejectTopic/topic/'
        };

        // -- public --//
        return {
            getResearcherTopics: getResearcherTopics,
            getStewardTopics: getStewardTopics,
            requestNewTopic: requestNewTopic,
            updateTopic: updateTopic,
            approveTopic: approveTopic,
            rejectTopic: rejectTopic
        };

        // -- private -- //

        /*  -- preserve method signature to allow flexibility in controller 
            -- note undefined passed in place of state -- 
        */
        function getResearcherTopics(skip, limit, state, sortBy, sortDirection) {
            var url = service.getUrl(urls.researcherTopics, skip,
                limit, undefined, sortBy, sortDirection);

            return $http.get(url)
                .then(parseTopics, onFail);
        }

        function getStewardTopics(skip, limit, state, sortBy, sortDirection) {
            var url = service.getUrl(urls.stewardTopics, skip, limit, state, sortBy, sortDirection);

            return $http.get(url)
                .then(parseTopics, onFail);
        }

        function processSuccess(result) {

        }

        function requestNewTopic(topic) {
            var url = service.getUrl(urls.researcherRequestAccess);

            return $http.post(url, topic)
                .then(processSuccess, onFail);
        }

        function updateTopic(topic) {
            var url = service.getUrl(urls.researchEditTopic) + topic.id;

            // -- todo: logic does not belong here? --//
            topic.name = topic.name.substring(0, 254);
            return $http.post(url, topic)
                .then(processSuccess, onFail);
        }

        function approveTopic(id) {
            var url = service.getUrl(urls.stewardApproveTopic) + id;
            return $http.post(url, {})
                .then(processSuccess, onFail);
        }

        function rejectTopic(id) {
            var url = service.getUrl(urls.stewardRejectTopic) + id;
            return $http.post(url, {})
                .then(processSuccess, onFail);
        }

        function parseTopics(result) {
            var topics = result.data.topics,
                skipped = result.data.skipped,
                totalCount = result.data.totalCount;

            return {
                topics: topics,
                numberSkipped: skipped,
                totalCount: totalCount
            };
        }

        function onFail(result) {
            window.alert('HTTP Request Fail: ' + result);
        }
    }
})();
