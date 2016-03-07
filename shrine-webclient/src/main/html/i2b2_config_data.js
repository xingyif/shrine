{
    urlProxy:       '/shrine-proxy/request",
    urlFramework:   'js-i2b2/',
    loginTimeout:   15, // in seconds
    username_label: 'test username:', //Username Label
    password_label: 'test password:', //Password Label
    clientHelpUrl:  'help/pdf/shrine-client-guide.pdf',
    networkHelpUrl: 'help/pdf/shrine-network-guide.pdf',
    wikiBaseUrl:    'https://open.med.harvard.edu/wiki/display/SHRINE/',

    // -------------------------------------------------------------------------------------------
    // THESE ARE ALL THE DOMAINS A USER CAN LOGIN TO
    lstDomains: [{
        domain:         'i2b2demo',
        name:           'Harvard',
        debug:          true,
        urlCellPM:      'https://shrine-dev1.catalyst/i2b2/services/PMService/',
        allowAnalysis:  true,
        isSHRINE:       true
    }]
    // -------------------------------------------------------------------------------------------
}
