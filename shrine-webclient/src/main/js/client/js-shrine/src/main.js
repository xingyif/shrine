import {stringIncludesPolyfill} from './includes-polyfill';
export function configure(aurelia) {
    aurelia.use
        .standardConfiguration()
        .developmentLogging()
        .feature('resources')
        .feature('views');

    aurelia.start()
        .then(stringIncludesPolyfill)
        .then(() => aurelia.setRoot('shell'));
}

