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
            getStewardTopics: getStewardTopics
        }; 
        
        // -- private -- //

        function getResearcherTopics(skip, limit, state, sortBy, sortDirection){
            var url = service.getUrl(urls.researcherTopics, skip, limit, state, sortBy, sortDirection);

            return $http.get(url)
                .then(parseTopics, onFail);
        }

        function getStewardTopics(skip, limit, state, sortBy, sortDirection) {
            var url = service.getUrl(urls.stewardTopics, skip, limit, state, sortBy, sortDirection);

            return $http.get(url)
                .then(parseTopics, onFail);            
        }


        function requestNewTopic(topic) {
            var url = service.getUrl(urls.researcherRequestAccess);

            return $http
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

        function processNewRequestSuccess(result) {
            var test = result;
        }

        function onFail(result) {
            alert("HTTP Request Fail: " + result);
        }

/*        topicsModel.getTopicsByState = function (role, skip, limit, state, sortBy, sortDirection) {
            var roleSegment = mdlSvc.getRoleSegment(role, $app.globals.UserRoles),
                url = mdlSvc.getURL(mdlSvc.url.base + roleSegment + URLS.FETCH, skip, limit, state, sortBy, sortDirection);

            return $http.get(url)
                .then(cacheTopics, onFail);
        };

        this.requestNewTopic = function (role, topic) {
            var roleSegment = mdlSvc.getRoleSegment(role, $app.globals.UserRoles),
                url = mdlSvc.getURL(mdlSvc.url.base +
                    roleSegment + URLS.REQUEST_ACCESS);

            return $http.post(url, topic)
                .then(processNewRequestSuccess, onFail);
        };

        TopicsMdl.prototype.getTopics = function (skip, limit, state, sortBy, sortDirection) {
            return svc.getTopicsByState(this.role, skip, limit, state, sortBy, sortDirection);
        };

        //@todo: clean this up same as above...use just one model.
        TopicsMdl.prototype.getTopics = function (skip, limit, state, sortBy, sortDirection) {
            return svc.getTopicsByState(this.role, skip, limit, state, sortBy, sortDirection);
        };

        TopicsMdl.prototype.requestNewTopic = function (topic) {
            topic.name = topic.name;
            return svc.requestNewTopic(this.role, topic);
        };

        model.approveTopic = function (topicId) {
            var url = mdlSvc.url.base + URLS.APPROVE_TOPIC + topicId;
            return $http.post(url, {});
        };

        model.rejectTopic = function (topicId) {
            var url = mdlSvc.url.base + URLS.REJECT_TOPIC + topicId;
            return $http.post(url, {});
        };


        function cacheTopics(result) {
            var topics = result.data.topics,
                skipped = result.data.skipped,
                totalCount = result.data.totalCount;

            return {
                topics: topics,
                numberSkipped: skipped,
                totalCount: totalCount
            };
        }

        function processNewRequestSuccess(result) {
            var test = result;
        }

        function onFail(result) {
            alert("HTTP Request Fail: " + result);
        }*/
    }
})();


