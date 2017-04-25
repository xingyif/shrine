var ShrineMockService = require('./shrine-mock.service');
var stewardData = require('./data/steward-data');

var StewardMockService = function (data) {
  this.data = data || stewardData;
  ShrineMockService.call(this);
}

/** inherit shrine mock methods **/
StewardMockService.prototype = Object.create(ShrineMockService.prototype);


StewardMockService.prototype.getTopics = function (numberOfResults, state, role) {
  state = state || 'Pending';
  role = role || 'DataSteward';

  var results = [];
  for (var i = 0; i < numberOfResults; i++) {
    var mockResult = this.data.topic;
    var user = (role === 'DataSteward') ? this.getMockDave() : this.getMockBen();
    mockResult.fullName = user.fullName;
    mockResult.role = user.roles;
    mockResult.userName = user.userName;
    mockResult.createdBy = user;
    mockResult.name = i + ' ' + mockResult.name;
    mockResult.state = state;
    mockResult.description = i + ' ' + mockResult.description;
    results.push(mockResult);
  }

  return results;
}



StewardMockService.prototype.getQueryHistory = function (numberOfResults, role) {
  var results = [];
  var topic = this.getTopics(1, 'Approved', role)[0];
  var user = (role === 'DataSteward') ? this.getMockDave() : this.getMockBen();

  for (var i = 0; i < numberOfResults; i++) {
    var result = this.data.queryResult;
    result.topic = topic;
    result.user = user;
    results.push(result);
  }

  return results;
}

//"https://shrine-dev1.catalyst:6443/steward/steward/statistics/topicsPerState?minDate=1476207028000&maxDate=1476898228000"
StewardMockService.prototype.getTopicsPerState = function () {
  return {
    data: {
      total: 0,
      topicsPerState: []
    }
  }
}


StewardMockService.prototype.getQueriesPerUser = function () {

  var totalQueries = 600;
  var users = {
    'Ben': 70,
    'Isha': 40,
    'Ty': 30,
    'Dave': 80,
    'Keith': 100,
    'Dr. Evil': 280
  };

  var jira1847Users = {
    'nn80': 50,
    'ij22': 52,
    'shrine': 51
  };

  var keys = Object.keys(jira1847Users);
  var queriesPerUser = [];

  for (var i = 0; i < keys.length; i++) {
    var user = this.getMockUser(keys[i]);
    var numberOfQueries = jira1847Users[keys[i]];
    queriesPerUser.push({
      _1: user,
      _2: numberOfQueries
    });
  }

  return {
      total: totalQueries,
      queriesPerUser: queriesPerUser
    }
}




StewardMockService.prototype.getJSONQueryHistory = function() {
  var queryRecords = this.data.queryHistory;
  return {
    'totalCount': 144,
    'skipped': 0,
    'queryRecords': queryRecords
  };
}


module.exports = new StewardMockService();