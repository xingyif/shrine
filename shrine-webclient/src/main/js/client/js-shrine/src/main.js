export function configure(aurelia) {
    aurelia.use
        .standardConfiguration()
        .developmentLogging();

    aurelia.start()
        .then(() => aurelia.setRoot('shell'));
        
    //@todo: cleanup.
    var shrine = {
        auth: sessionStorage.getItem('shrine.auth')
    };
    sessionStorage.removeItem('shrine.auth');
    aurelia.use.instance('shrine', shrine);
}