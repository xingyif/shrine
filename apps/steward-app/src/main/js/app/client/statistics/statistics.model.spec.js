(function () {
    'use strict';

    describe('statistics model tests', StatisticsModelSpec);

    function StatisticsModelSpec() {

        var statisticsModel, $httpBackend, stewardService;

        function setup() {
            module('shrine.steward.statistics');

            inject(function (_$httpBackend_, _StatisticsModel_, _StewardService_) {
                $httpBackend = _$httpBackend_;
                statisticsModel = _StatisticsModel_;
                stewardService = _StewardService_;
                $httpBackend.whenGET(/\.html$/).respond('');
            });
        }

        beforeEach(setup);

        it('getQueriesPerUser Test', function () {

            var mockQueriesPerUser = [
                {
                    _1: {
                        fullname: 'Steward Test Researcher Ben',
                        roles: ['Researcher'],
                        username: 'ben'
                    },
                    _2: 10
                }
            ];

            var mockData = {
                queriesPerUser: mockQueriesPerUser,
                total: 10
            };

            var skip, limit, state, sortBy, sortDirection, startDate = 0, endDate = 300000;
            var url = stewardService.getUrl('steward/statistics/queriesPerUser', skip, limit, state,
                sortBy, sortDirection, startDate, endDate);

            $httpBackend.expectGET(url).respond(mockData);


            statisticsModel.getQueriesPerUser(startDate,endDate)
                .then(function (data) {
                    expect(data.total).toBe(mockData.total);
                });

            $httpBackend.flush();
        });
    }
})();
