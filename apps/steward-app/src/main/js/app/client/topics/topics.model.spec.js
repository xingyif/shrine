(function () {
    'use strict';

    describe('topics model tests', TopicsModelSpec);

    function TopicsModelSpec() {

        var topicsModel, $httpBackend, topicsService;

        function setup() {
            module('shrine.steward.topics');

            inject(function (_$httpBackend_, _TopicsModel_, _TopicsService_) {
                $httpBackend = _$httpBackend_;
                topicsModel = _TopicsModel_;
                topicsService = _TopicsService_;
                $httpBackend.whenGET(/\.html$/).respond('');
            });
        }

        beforeEach(setup);

        it('topicsModel.getStewardTopics - test', function () {
            var mockTopic = {
                changeDate: 1444234776566,
                fullName: 'Steward Test Steward Dave',
                roles: ['DataSteward', 'Researcher'],
                userName: 'dave',
                createDate: 1443816532550,
                createdBy: {
                    fullName: 'Steward Test Researcher Ben',
                    roles: ['Researcher'],
                    userName: 'ben'
                },
                description: 'Dave\'s non proident, sunt in culpa qui officia deserunt mollit anim id est laborum',
                id: 8,
                name: 'Dave\'s Phantom Limb Pain in Recent Amputees',
                state: 'Approved'
            };

            var mockData = {
                topics: [mockTopic],
                numberSkipped: 0,
                totalCount: 1
            };

            var expectedResult = {
            }

            var url = topicsService.getUrl('steward/topics', mockData.numberSkipped, mockData.totalCount);
            $httpBackend.expectGET(url).respond(mockData);

            topicsModel.getStewardTopics(mockData.numberSkipped, mockData.totalCount)
                .then(function (data) {
                    expect(data.topics.length).toBe(mockData.totalCount);
                });
            $httpBackend.flush();
        });

        it('topicsModel.getResearcherTopics - test', function () {
            var mockTopic = {
                changeDate: 1444234776566,
                fullName: 'Steward Test Researcher Ben',
                roles: ['Researcher'],
                userName: 'ben',
                createDate: 1443816532550,
                createdBy: {
                    fullName: 'Steward Test Steward Dave',
                    roles: ['DataSteward', 'Researcher'],
                    userName: 'dave'
                },
                description: 'Ben\'s non proident, sunt in culpa qui officia deserunt mollit anim id est laborum',
                id: 8,
                name: 'Ben\'s Phantom Limb Pain in Recent Amputees',
                state: 'Approved'
            };

            var mockData = {
                topics: [mockTopic],
                numberSkipped: 0,
                totalCount: 1
            };

            var url = topicsService.getUrl('researcher/topics', mockData.numberSkipped, mockData.totalCount);
            $httpBackend.expectGET(url).respond(mockData);

            topicsModel.getResearcherTopics(mockData.numberSkipped, mockData.totalCount)
                .then(function (data) {
                    expect(data.topics.length).toBe(mockData.totalCount);
                });
            $httpBackend.flush();
        });
    }
})();
