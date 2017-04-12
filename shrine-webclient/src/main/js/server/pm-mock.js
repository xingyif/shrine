
/*
This is a first stab at a i2b2 pm-mock 
  @todo:  refactor/cleanup this module.
*/
var express = require('express');
var cors = require('cors');
var bodyParser = require('body-parser');
require('body-parser-xml')(bodyParser);
var app = express();
var bodyParser = require('body-parser');
var port = process.env.PORT || 6443;
var router = express.Router();
var fs = require('fs');
var isAuthorized = false;


function start(dir) {
  app.use(bodyParser.urlencoded({ extended: true }));
  app.use(bodyParser.json());
  app.use(bodyParser.xml({
    limit: '1GB',   // Reject payload bigger than 1 MB 
    xmlParseOptions: {
      normalize: true,     // Trim whitespace inside text nodes 
      normalizeTags: true, // Transform tags to lowercase 
      explicitArray: false // Only put nodes in array if >1 
    }
  }));

  app.use(cors());
  app.use('/shrine-proxy/request', router);
  app.listen(port);
  console.log('I2B2SERVER Mock Server started on port: ' + port);

  // -- routes --//

  router.post('/', function (req, res) {
    var requestType = parseRequest(req);
    var fileName = getFilename(requestType);
    var xml = fs.readFileSync('./' + dir+ '/i2b2-xml/' + fileName);
    res.header('Content-Type', 'text/xml').send(xml);
  });

  router.get('/shrine-metadata/data', function(req, res) {
    res.json('steward@steward.com');
  });

  router.get('/shrine/api/previous-queries', (req, res) => {
    const result = require('./data/curl-async-queries');
    res.json(result);
  })
}

/*
 Any request with an XML payload will be parsed and a JavaScript requestect produced on req.body 
 corresponding to the request payload. 
 This is messy refactor.
*/
function parseRequest(request) {
  request = request.body;
  var requestTypes = ['i2b2:request', 'ns2:request', 'ns3:request', 'ns6:request'];
  var requestType = requestTypes.find(t => request[t] !== undefined);
  request = (request[requestType] && request[requestType].message_body) ?
    request[requestType].message_body : null;

  return parseBodyRequest(request);
}

function parseBodyRequest(request) {
  var bodyTypes = ['pm:get_user_configuration', 'ns4:get_categories', 'ns4:get_schemes', 'ns7:sheriff_header', 'ns4:psmheader'];
  var bodyType = bodyTypes.find(t => request[t] !== undefined);

  if (bodyType == 'ns4:psmheader') {
    bodyType = request['ns4:psmheader'].request_type;
  }

  return bodyType;
}

function getFilename(value) {
  let fileMap = {
    'pm:get_user_configuration': 'getUserAuth.xml',
    'ns4:get_categories': 'GetCategories.xml',
    'ns4:get_schemes': 'GetSchemes.xml',
    'ns7:sheriff_header': 'getUserAuth.xml',
    'CRC_QRY_getResultType': 'getQRY_getResultType.xml',
    'CRC_QRY_getQueryMasterList_fromUserId': 'getQueryMasterList_fromUserId.xml'
  };

  return fileMap[value];
}

module.exports = {start: start};