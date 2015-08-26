{
    urlProxy: "/shrine-proxy/request",
        urlFramework: "js-i2b2/",
    loginTimeout: 15, // in seconds
    //JIRA|SHRINE-519:Charles McGow
    username_label:"test username:", //Username Label
    password_label:"test password:", //Password Label
    clientHelpUrl:'help/pdf/shrine-client-guide.pdf',
    networkHelpUrl:'help/pdf/shrine-network-guide.pdf',
    //JIRA|SHRINE-519:Charles McGow
    // -------------------------------------------------------------------------------------------
    // THESE ARE ALL THE DOMAINS A USER CAN LOGIN TO
    lstDomains: [
    {
        domain: "i2b2demo",
        name: "Harvard",
        debug: true,
        urlCellPM: "http://shrine-qa1.hms.harvard.edu/i2b2/services/PMService/",
        //urlCellPM: "http://192.168.169.131/i2b2/services/PMService/",
        allowAnalysis: true/*,
        isSHRINE: true*/
    }
]
    // -------------------------------------------------------------------------------------------
}