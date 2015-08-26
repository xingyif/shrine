angular
    .module("topics-model", ["topics-model.private"])
    .service("TopicsModelFactory", ['$app', 'Role1TopicsMdl', 'Role2TopicsMdl', function ($app, Role1TopicsMdl, Role2TopicsMdl) {
        this.getInstance = function (role) {
            var instance = (role === $app.globals.UserRoles.ROLE1) ? new Role1TopicsMdl() : new Role2TopicsMdl();
            return instance;
        };
    }]);
angular
    .module("topics-model.private", ['model-service'])
    .service("TopicsModelSvc", ['$http', 'ModelService', '$app', function ($http, mdlSvc, $app) {
        var topics      = [],
            URLS        = {
                FETCH:          "/topics",
                REQUEST_ACCESS: "/requestTopicAccess"
            };

        function processNewRequestSuccess(result) {
            var test = result;
        }

        function onFail(result) {
            alert("HTTP Request Fail: " + result);
        }

        function cacheTopics(result) {
            var topics      = result.data.topics,
                skipped     = result.data.skipped,
                totalCount  = result.data.totalCount;

            return {
                topics:         topics,
                numberSkipped:  skipped,
                totalCount:     totalCount
            };
        }

        this.getTopics = function (role, skip, limit, sortBy, sortDirection) {
            var roleSegment = mdlSvc.getRoleSegment(role, $app.globals.UserRoles),
                url         = mdlSvc.getURL(mdlSvc.url.base + roleSegment + URLS.FETCH, skip, limit, sortBy, sortDirection);

            return $http.get(url)
                .then(cacheTopics, onFail);
        };

        this.getTopicsByState = function (role, skip, limit, state, sortBy, sortDirection) {
            var roleSegment = mdlSvc.getRoleSegment(role, $app.globals.UserRoles),
                url         = mdlSvc.getURL(mdlSvc.url.base + roleSegment + URLS.FETCH, skip, limit, state, sortBy, sortDirection);

                return $http.get(url)
                    .then(cacheTopics, onFail);
        };

        this.requestNewTopic =  function (role, topic) {
            var roleSegment = mdlSvc.getRoleSegment(role, $app.globals.UserRoles),
                url         = mdlSvc.getURL(mdlSvc.url.base +
                    roleSegment + URLS.REQUEST_ACCESS);

            return $http.post(url, topic)
                .then(processNewRequestSuccess, onFail);
        };
    }])
    .service('Role2TopicsMdl', ['TopicsModelSvc', '$app', function (svc, $app) {

        function TopicsMdl() {
            this.role   = $app.globals.UserRoles.ROLE2;
        }

        TopicsMdl.prototype.getTopics =  function (skip, limit, state, sortBy, sortDirection) {
            return svc.getTopicsByState(this.role, skip, limit, state, sortBy, sortDirection);
        };

        return TopicsMdl;
    }])
    .service("Role1TopicsMdl", ['TopicsModelSvc', '$app', 'Role2TopicsMdl', function (svc, $app, superMdl) {
        function TopicsMdl() {
            this.role = $app.globals.UserRoles.ROLE1;
        }

        //@todo: clean this up same as above...use just one model.
        TopicsMdl.prototype.getTopics =  function (skip, limit, state, sortBy, sortDirection) {
            return svc.getTopicsByState(this.role, skip, limit, state, sortBy, sortDirection);
        };

        TopicsMdl.prototype.requestNewTopic =  function (topic) {
            return svc.requestNewTopic(this.role, topic);
        };

        return TopicsMdl;
    }])
    .service("Role2TopicDetailMdl", ["$http", "ModelService", function ($http, mdlSvc) {
        var model = this,
            URLS = {
                APPROVE_TOPIC: "steward/approveTopic/topic/",
                REJECT_TOPIC: "steward/rejectTopic/topic/"
            };

        model.approveTopic = function (topicId) {
            var url = mdlSvc.url.base + URLS.APPROVE_TOPIC + topicId;
            return $http.post(url, {});
        };

        model.rejectTopic = function(topicId) {
            var url = mdlSvc.url.base + URLS.REJECT_TOPIC + topicId;
            return $http.post(url, {});
        };

        return model;
    }])
    .service("Role1TopicDetailMdl", ["$http", "ModelService", function ($http, mdlSvc) {

        /*
         curl -w " %{http_code}\n" -u ben:kapow -X POST "http://localhost:8080/steward/researcher/editTopicRequest/1" -H "Content-Type: application/json" -d '{"name":"KidneyStudy","description":"Kidney Study you should approve"}'
         */
        var model = this,
            URLS = {
                UPDATE_TOPIC: "researcher/editTopicRequest/"
            };

        model.updateTopic = function (id, name, description) {
            var url = mdlSvc.url.base + URLS.UPDATE_TOPIC + id;
            return $http.post(url, {
                name:        name,
                description: description
            });
        };

        return model;
    }]);




