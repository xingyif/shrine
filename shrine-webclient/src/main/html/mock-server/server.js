
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
  // Any request with an XML payload will be parsed 
  // and a JavaScript object produced on req.body 
  // corresponding to the request payload. 
  
 //console.log(req.body);
 var xml = fs.readFileSync('./getUserAuth.xml');

 
  res.header('Content-Type','text/xml').send(xml);
  

  //res.send('<xml>i2b2 success</xml>');
});