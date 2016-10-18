
var express = require('express');       
var cors = require('cors');
var app = express();                
var bodyParser = require('body-parser');
var service = require('./steward-mock.service');
var port = process.env.PORT || 6443;
var router = express.Router();

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(cors());
app.use('/steward', router);
app.listen(port);
console.log('Steward Mock Server started on port: ' + port);

// -- routes --//

router.get('/user/whoami', function (req, res) {
  var user = service.parseAuthHeader(req);
  var response = "AuthenticationFailed";

  if (user.username == 'ben' && user.password == 'kapow') {
    response = { userName: 'ben', fullName: "ben", roles: ["Researcher"] };
  }

  else if (user.username == 'dave' && user.password == 'kablam') {
    response = { userName: 'dave', fullName: "dave", roles: ["Researcher", "DataSteward"] };
  }

  res.json(response);
});

router.get('/steward/topics', function (req, res) {
  var user = service.parseAuthHeader(req);
  var role = 'DataSteward';
  var topics = service.getTopics(20, 'Pending', role);
  var response = {
    skipped: 0,
    totalCount: 50,
    userId: user.username
  };

  response.topics = topics;
  res.json(response);
});

router.get('/researcher/topics', function (req, res) {
  var user = service.parseAuthHeader(req);
  var response = {
    skipped: 0,
    totalCount: 50,
    userId: user.username
  };
  var topics = service.getTopics(20, 'Pending');

  response.topics = topics;
  res.json(response);
});

router.post('/researcher/editTopicRequest/:id', function (req, res) {
  res.end('ok');
});

router.post('/researcher/requestTopicAccess', function (req, res) {
  var topic = req.body.topic;
  res.end('ok');
});

router.post('/steward/approveTopic/topic/:id', function (req, res) {
  res.end('ok');
});

router.post('/steward/rejectTopic/topic/:id', function (req, res) {
  res.end('ok');
});

router.get('/researcher/queryHistory/topic/:id', function (req, res) {
  var response = {
    skipped: 0,
    totalCount: 970
  };
  
  response.queryRecords = service.getQueryHistory(response.totalCount);
  res.json(response);
});

router.get('/researcher/queryHistory', function (req, res) {
  var response = {
    skipped: 0,
    totalCount: 970
  };
  
  response.queryRecords = service.getQueryHistory(response.totalCount);
  res.json(response);
});

router.get('/steward/queryHistory/topic/:id', function (req, res) {
  var response = {
    skipped: 0,
    totalCount: 970
  };
  
  response.queryRecords = service.getQueryHistory(response.totalCount, 'DataSteward');
  res.json(response);
});

router.get('/steward/queryHistory', function (req, res) {
  var response = {
    skipped: 0,
    totalCount: 970
  };
  
  response.queryRecords = service.getQueryHistory(response.totalCount, 'DataSteward');
  res.json(response);
});

router.get('/steward/queryHistory/user/:username', function (req, res) {
  var response = service.getJSONQueryHistory();
  res.json(response);
});

router.get('/steward/statistics/queriesPerUser', function (req, res) {
  var response = service.getQueriesPerUser();

  res.json(response);
});

router.get('/steward/statistics/topicsPerState', function (req, res) {
  var response = service.getTopicsPerState();
  res.json(response);
});



