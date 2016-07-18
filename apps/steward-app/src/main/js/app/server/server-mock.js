
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


// more routes for our API will happen here

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
  }

}
