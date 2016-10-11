/*(function () {
    'use strict';

    describe('statistics model tests', StatisticsModelSpec);

    function StatisticsModelSpec() {

        var statisticsModel, $httpBackend, statisticsService;

        function setup() {
            module('shrine.steward.statistics');

            inject(function (_$httpBackend_, _StatisticsModel_, _StatisticsService_) {
                $httpBackend = _$httpBackend_;
                statisticsModel = _StatisticsModel_;
                statisticsService = _StatisticsService_;
                $httpBackend.whenGET(/\.html$/).respond('');
            });
        }

        beforeEach(setup);

        it('historyModel.getStewardHistory - test', function () {

            var mockHistory = {
                date: 1440773077637,
                externalId: -1,
                name: '3 years old@10:44:20',
                queryContents: '<queryDefinition><name>3 years old@10:44:20</name><expr><term>shrine expression here.</term></expr></queryDefinition>',
                stewardId: 2,
                stewardResponse: 'Approved',
                topic: {},
                user: {}
            };

            var mockData = {
                queryRecords: [mockHistory],
                numberSkipped: 0,
                totalCount: 1
            };

            var expectedResult = {
            }

            var url = historyService.getUrl('steward/queryHistory', mockData.numberSkipped, mockData.totalCount);
            $httpBackend.expectGET(url).respond(mockData);

            historyModel.getStewardHistory(mockData.numberSkipped, mockData.totalCount)
                .then(function (data) {
                    expect(data.queryRecords.length).toBe(mockData.totalCount);
                });
            $httpBackend.flush();
        });

        it('historyModel.getResearcherHistory - test', function () {
            var mockHistory = {
                date: 1440773077637,
                externalId: -1,
                name: '3 years old@10:44:20',
                queryContents: '<queryDefinition><name>3 years old@10:44:20</name><expr><term>shrine expression here.</term></expr></queryDefinition>',
                stewardId: 2,
                stewardResponse: 'Approved',
                topic: {},
                user: {}
            };

            var mockData = {
                queryRecords: [mockHistory],
                numberSkipped: 0,
                totalCount: 1
            };

            var expectedResult = {
            }

            var url = historyService.getUrl('researcher/queryHistory', mockData.numberSkipped, mockData.totalCount);
            $httpBackend.expectGET(url).respond(mockData);

            historyModel.getResearcherHistory(mockData.numberSkipped, mockData.totalCount)
                .then(function (data) {
                    expect(data.queryRecords.length).toBe(mockData.totalCount);
                });
            $httpBackend.flush();
        });
    }
})();
*/