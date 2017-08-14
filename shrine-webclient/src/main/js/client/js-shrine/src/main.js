export function configure(aurelia) {
    aurelia.use
        .standardConfiguration()
        .developmentLogging()
        .feature('resources')
        .feature('views');

    aurelia.start()
        .then(() => aurelia.setRoot('shell'));
}

