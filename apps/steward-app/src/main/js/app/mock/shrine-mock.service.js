var ShrineMockService = function () {
    this.name = 'ShrineMockService';
};

/**
 * http://stackoverflow.com/questions/5951552/basic-http-authentication-in-node-js
 */
ShrineMockService.prototype.parseAuthHeader = function(req) {
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

ShrineMockService.prototype.getMockBen = function () {
  return {
    fullName: 'Steward Test Researcher Ben',
    roles: ['Researcher'],
    username: 'ben'
  };
};

ShrineMockService.prototype.getMockDave = function() {
  return {
    fullName: 'Steward Test Steward Dave',
    roles: ['DataSteward', 'Researcher'],
    userName: 'dave'
  };
}

ShrineMockService.prototype.getMockUser = function (name) {
  var user = this.getMockBen();
  user.userName = name;
  user.fullName = name;
  return user;
}

module.exports  = ShrineMockService;