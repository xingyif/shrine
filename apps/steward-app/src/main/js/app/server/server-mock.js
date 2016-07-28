
// BASE SETUP
// =============================================================================
//https://scotch.io/tutorials/build-a-restful-api-using-node-and-express-4
// call the packages we need
var express = require('express');        // call express
var cors = require('cors');
var app = express();                 // define our app using express
var bodyParser = require('body-parser');

// configure app to use bodyParser()
// this will let us get the data from a POST
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(cors());

var port = process.env.PORT || 8080;        // set our port

// ROUTES FOR OUR API
// =============================================================================
var router = express.Router();              // get an instance of the express Router



// test route to make sure everything is working (accessed at GET http://localhost:8080/api)
router.get('/user/whoami', function (req, res) {
  var user = parseAuthHeader(req);
  var response = "AuthenticationFailed";

  if (user.username == 'ben' && user.password == 'kapow') {
    response = { userName: 'ben', fullName: "ben", roles: ["Researcher"] };
  }

  else if (user.username == 'dave' && user.password == 'kablam') {
    response = { userName: 'dave', fullName: "dave", roles: ["Researcher", "DataSteward"] };
  }

  res.json(response);
});

//"https://shrine-dev1.catalyst:6443/steward/steward/topics?skip=0&limit=20&state=Pending&sortBy=changeDate&sortDirection=ascending"
//"https://shrine-dev1.catalyst:6443/steward/steward/topics?skip=0&limit=20&state=Approved&sortBy=changeDate&sortDirection=ascending"
//"https://shrine-dev1.catalyst:6443/steward/steward/topics?skip=0&limit=20&state=Rejected&sortBy=changeDate&sortDirection=ascending"
// more routes for our API will happen here
/*
*/

// test route to make sure everything is working (accessed at GET http://localhost:8080/api)
router.get('/steward/topics', function (req, res) {
  var user = parseAuthHeader(req);
  var response = {
    skipped: 0,
    totalCount: 5,
    userId: user.username
  };

  var topics = getMockStewardTopics(response.totalCount, 'Pending');

  response.topics = topics;
  res.json(response);
});

router.get('/researcher/topics', function (req, res) {
  var user = parseAuthHeader(req);
  var response = {
    skipped: 0,
    totalCount: 5,
    userId: user.username
  };

  var topics = getMockStewardTopics(response.totalCount, 'Pending', 'Researcher');

  response.topics = topics;
  res.json(response);
});


//https://shrine-dev1.catalyst:6443/steward/researcher/editTopicRequest/17

// test route to make sure everything is working (accessed at GET http://localhost:8080/api)
//https://codeforgeek.com/2014/09/handle-get-post-request-express-4/
router.post('/researcher/editTopicRequest/:id', function (req, res) {
  res.end('ok');
});

// test route to make sure everything is working (accessed at GET http://localhost:8080/api)
router.post('/researcher/requestTopicAccess', function (req, res) {
  var topic = req.body.topic;
  res.end('ok');
});

//https://codeforgeek.com/2014/09/handle-get-post-request-express-4/
router.post('/steward/approveTopic/topic/:id', function (req, res) {
  res.end('ok');
});

router.post('/steward/rejectTopic/topic/:id', function (req, res) {
  res.end('ok');
});

//Query History Methods:
//"https://shrine-dev1.catalyst:6443/steward/researcher/queryHistory/topic/8?skip=0&limit=20&sortBy=date&sortDirection=ascending"
router.get('/researcher/queryHistory/topic/:id', function (req, res){
  var response = {
    skipped: 0,
    totalCount: 5
  };
    var queryRecords = getResearcherMockQueryHistory(response.totalCount);
    response.queryRecords = queryRecords;

    res.json(response);
});


//"https://shrine-dev1.catalyst:6443/steward/researcher/queryHistory?skip=0&limit=20&sortBy=date&sortDirection=ascending"
//Query History Methods:
//"https://shrine-dev1.catalyst:6443/steward/researcher/queryHistory/topic/8?skip=0&limit=20&sortBy=date&sortDirection=ascending"
router.get('/researcher/queryHistory/', function (req, res){
  var response = {
    skipped: 0,
    totalCount: 5
  };
    var queryRecords = getResearcherMockQueryHistory(response.totalCount);
    response.queryRecords = queryRecords;

    res.json(response);
});
      

//"https://shrine-dev1.catalyst:6443/steward/researcher/queryHistory/topic/8?skip=0&limit=20&sortBy=date&sortDirection=ascending"
router.get('/steward/queryHistory/topic/:id', function (req, res){
  var response = {
    skipped: 0,
    totalCount: 5
  };
    var queryRecords = getStewardMockQueryHistory(response.totalCount);
    response.queryRecords = queryRecords;

    res.json(response);
});

//"https://shrine-dev1.catalyst:6443/steward/researcher/queryHistory/topic/8?skip=0&limit=20&sortBy=date&sortDirection=ascending"
router.get('/steward/queryHistory/', function (req, res){
  var response = {
    skipped: 0,
    totalCount: 5
  };
    var queryRecords = getStewardMockQueryHistory(response.totalCount);
    response.queryRecords = queryRecords;

    res.json(response);
});


// REGISTER OUR ROUTES -------------------------------
// all of our routes will be prefixed with /api
app.use('/steward', router);

// START THE SERVER
// =============================================================================
app.listen(port);
console.log('Magic happens on port ' + port);

/**
 * http://stackoverflow.com/questions/5951552/basic-http-authentication-in-node-js
 */
function parseAuthHeader(req) {
  var header = req.headers['authorization'] || '',        // get the header
    token = header.split(/\s+/).pop() || '',            // and the encoded auth token
    auth = new Buffer(token, 'base64').toString(),    // convert from base64
    parts = auth.split(/:/),                          // split on colon
    username = parts[0],
    password = parts[1];

  return {
    username: username,
    password: password
  };
}

function getMockStewardTopics(numberOfResults, state, role) {
  state = state || 'Pending';
  role = role || 'DataSteward';
  var mockResult;

  var results = [];
  for (var i = 0; i < numberOfResults; i++) {

    if (role == 'DataSteward') {
      mockResult = getMockStewardTopicResult();
    } else {
      mockResult = getMockResearcherTopicResult();
    }

    mockResult.name = i + ' ' + mockResult.name;
    mockResult.state = state;
    mockResult.description = i + ' ' + mockResult.description;

    results.push(mockResult);
  }

  return results;
}

function getStewardMockQueryHistory(numberOfResults) {
  var results = [];
  var topic = getMockStewardTopicResult()
  var user = getMockDave();

  for(var i = 0; i < numberOfResults; i ++) {
    var result = getMockQueryResult(user, topic);
    results.push(result);
  }

  return results;
}


function getResearcherMockQueryHistory(numberOfResults) {
  var results = []
  var topic = getMockResearcherTopicResult();
  var user = getMockBen();


  for(var i = 0; i < numberOfResults; i ++) {
    var result = getMockQueryResult(user, topic);
    results.push(result);
  }

  return results;
}

function getMockQueryResult(user, topic) {
  return {
    date: 1440773077637,
    externalId: -1,
    name: '3 years old@10:44:20',
    queryContents: '<queryDefinition><name>3 years old@10:44:20</name><expr><term>\\SHRINE\SHRINE\Demographics\Age\0-9 years old\3 years old\</term></expr></queryDefinition>',
    stewardId: 2,
    stewardResponse: 'Approved',
    topic: topic,
    user: user
  };
}

function getMockBen() {
  return {
    fullName: 'Steward Test Researcher Ben',
    roles: ['Researcher'],
    userName: 'ben'
  };
}

function getMockDave() {
  return {
    fullName: 'Steward Test Steward Dave',
    roles: ['DataSteward', 'Researcher'],
    userName: 'dave'
  };
}

function getMockStewardTopicResult(user, creator) {
  var ben = getMockBen();
  var dave = getMockDave();

  return {
    changeDate: 1444234776566,
    fullName: dave.fullName,
    roles: dave.roles,
    userName: dave.userName,
    createDate: 1443816532550,
    createdBy: ben,
    description: 'Dave\'s non proident, sunt in culpa qui officia deserunt mollit anim id est laborum',
    id: 8,
    name: 'Dave\'s Phantom Limb Pain in Recent Amputees',
    state: 'Approved'
  };
}


function getMockResearcherTopicResult() {
  var ben = getMockBen();
  var dave = getMockDave();

  return {
    changeDate: 1444234776566,
    fullName: ben.fullName,
    roles: ben.roles,
    userName: ben.userName,
    createDate: 1443816532550,
    createdBy: dave,
    description: 'Ben\'s non proident, sunt in culpa qui officia deserunt mollit anim id est laborum',
    id: 8,
    name: 'Ben\'s Phantom Limb Pain in Recent Amputees',
    state: 'Approved'
  };
}
