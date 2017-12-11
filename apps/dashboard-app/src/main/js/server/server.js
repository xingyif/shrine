
const express = require('express');       
const cors = require('cors');
const app = express();                
const bodyParser = require('body-parser');
const router = express.Router();
const PORT = 6443
const BASE = '/shrine-dashboard';

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
app.use(cors());
app.use(BASE, router);
app.listen(PORT);
console.log('SHRINE Dashboard Mock Server started on port: ' + PORT);

const parseAuthHeader = req => {
  const token = (req.headers['authorization'] || '')
    .split(/\s+/).pop() || '';
  
  const parts = (new Buffer(token, 'base64').toString())
    .split(/:/);

  return {
    username: parts[0],
    password: parts[1]
  }

/*
  var header = req.headers['authorization'] || '',        // get the header
  token = header.split(/\s+/).pop() || '',            // and the encoded auth token
  auth = new Buffer(token, 'base64').toString(),    // convert from base64
  parts = auth.split(/:/),                          // split on colon
  username = parts[0],
  password = parts[1];

  return {
    username: username,
    password: password
  };*/
}


// -- routes --//
//http://localhost:6443/shrine-dashboard/user/whoami
router.get('/user/whoami', function (req, res) {
  const user = parseAuthHeader(req);
  let response = { userName: 'ben', fullName: "ben", roles: ["Researcher"] };

  if (user.username == 'dave') 
    response = { userName: 'dave', fullName: "dave", roles: ["Researcher", "DataSteward"] };
  
  else if(user.username == 'fail') response = "AuthenticationFailed";

  res.json(response);
});

/*
router.get('/steward/topics', function (req, res) {
  var user = service.parseAuthHeader(req);
  var role = 'DataSteward';
  var topics = service.getTopics(20, 'Pending', role);
  var response = {
    skipped: 0,
    totalCount: 50,
    userId: user.userName
  };

  response.topics = topics;
  res.json(response);
});

router.get('/researcher/topics', function (req, res) {
  var user = service.parseAuthHeader(req);
  var response = {
    skipped: 0,
    totalCount: 50,
    userId: user.userName
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

router.get('/steward/queryHistory/user/:userName', function (req, res) {
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
*/


