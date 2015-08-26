angular
    .module("topic-detail", ['topic-detail-model'])
    .controller('TopicDetailCtrl', ['$scope', '$modalInstance', 'modalData', 'modalCallback', 'Role2TopicDetailMdl', 'Role1TopicDetailMdl', '$app', function ($scope, $modalInstance, modalData, modalCallback, Role2TopicDetailMdl, Role1TopicDetailMdl, $app) {

        $scope.roles        = $app.globals.UserRoles;
        $scope.formatDate   = $app.utils.utcToMMDDYYYY;
        $scope.currentTopic = modalData;
        $scope.tabState     = 'description';
        $scope.modalCallback= modalCallback;
        $scope.selectedTab  = $scope.tabState;

        $scope.ok = function (id) {
            if($scope.currentTopic.state === "Pending") {
                $modalInstance.close();
                return;
            }

            (($scope.currentTopic.state == "Approved") ?
                Role2TopicDetailMdl.approveTopic(id) :
                Role2TopicDetailMdl.rejectTopic(id))
                .then(function (result) {
                    $scope.modalCallback();
                    $modalInstance.close(result);
                });
        };

        $scope.update = function (id, name, description) {
            Role1TopicDetailMdl.updateTopic(id, name, description)
                .then( function (result) {
                    $scope.modalCallback();
                    $modalInstance.close(result);
                });
        };

        $scope.setState = function (state) {
            if ($scope.isEditable() === true) {
                $scope.tabState = state;
            }
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

        $scope.isEditable =  function () {
            return ($app.globals.currentUser.roles[0] === $scope.roles.ROLE2) || ($scope.currentTopic.state === "Pending");
        };

        $scope.canViewHistory = function () {
            var canView =  ($app.globals.currentUser.roles[0] === $scope.roles.ROLE2 && $scope.currentTopic.state !== "Pending") || ($scope.currentTopic.state === "Approved");
            return canView;
        };

    }])

    .directive("topicEdit", function () {
        return {
            restrict:    "A",
            templateUrl: "src/app/common/topic-detail/edit/edit.tpl.html",
            replace:     true
        };
    })
    .directive("topicDescription", function () {
        return {
            restrict:    "A",
            templateUrl: "src/app/common/topic-detail/description/description-per-role.tpl.html",
            replace:     true
        };
    });