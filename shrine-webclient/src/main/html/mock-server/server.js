
//http://localhost:8000/shrine-proxy/request
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
  var requestType = parseRequestType(req);
  var fileName = getFilename(requestType);

  console.log('\n\n');
  console.log(req.body);
   

  var xml = fs.readFileSync('./getUserAuth.xml');
  res.header('Content-Type', 'text/xml').send(xml);
});



function getFilename (requestType) {
  switch (requestType) {

    case 'i2b2:request':
      return 'getUserAuth.xml';
      break;

    case 'CRC_QRY_getResultType':
      return 'getQRY_getResultType.xml'
      break;
    
    case 'CRC_QRY_getQueryMasterList_fromUserId':
      return 'getQueryMasterList_fromUserId.xml'
      break;
  
    default:
      return null;
      break;
  }
}


/*
 Any request with an XML payload will be parsed and a JavaScript object produced on req.body 
 corresponding to the request payload. 
 This is messy refactor.
*/
function parseRequestType(req) {

  if(req.body['i2b2:request']){
    return 'i2b2:request';
  }

  if (req.body['ns6:request']) {
    return req.body['ns6:request'].message_body['ns4:psmheader'].request_type;
  }

  if (req.body['ns6:request']) {
    return req.body['ns6:request'].message_body['ns4:psmheader'].request_type;
  }

}