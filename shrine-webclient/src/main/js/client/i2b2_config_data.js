{
  //urlProxy: "http://localhost:6443/shrine-proxy/request/",
  urlProxy: "/shrine-proxy/request",	
  urlFramework: "js-i2b2/",
  loginTimeout: 15, // in seconds
  //JIRA|SHRINE-519:Charles McGow
        username_label:"SHRINE Username:", //Username Label
        password_label:"SHRINE Password:", //Password Label
        clientHelpUrl: 'help/pdf/shrine-client-guide.pdf',
        networkHelpUrl:'help/pdf/shrine-network-guide.pdf',
        wikiBaseUrl:    'https://open.med.harvard.edu/wiki/display/SHRINE/',
        obfuscation: 10,
        resultName: "patients",
  //JIRA|SHRINE-519:Charles McGow
  // -------------------------------------------------------------------------------------------
  // THESE ARE ALL THE DOMAINS A USER CAN LOGIN TO
  lstDomains: [
                { domain: "i2b2demo",
                  name: "SHRINE",
                  urlCellPM: "http://services.i2b2.org/i2b2/services/PMService/",
                  allowAnalysis: true,
                  debug: true,
                  isSHRINE: true
                }
  ]
  // -------------------------------------------------------------------------------------------
}
