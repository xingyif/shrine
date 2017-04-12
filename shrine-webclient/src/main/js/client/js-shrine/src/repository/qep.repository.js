import { inject } from 'aurelia-framework';
import { HttpClient } from 'aurelia-fetch-client';
import 'fetch';

@inject(HttpClient)
export class QEPRepository {

    constructor(http) {
        http.configure(config => {
            config
                .useStandardConfiguration()
                .withBaseUrl(this.url)
                .withDefaults({
                    headers: {
                        'Authorization': 'Basic ' + this.auth
                    }
                });
        });

        this.http = http;
    }

    get url() {
        const url = document.URL;
        const service = ':6443/shrine-metadata/';
        return url.substring(0, url.lastIndexOf(':')) + service;
    }

    get auth() {
        const auth = sessionStorage.getItem('shrine.auth');
        sessionStorage.removeItem('shrine.auth');
        return auth;
    }

    fetchPreviousQueries() {
        return this.http.fetch('qep/queryResults')
            .then(response => response.json())
            .catch(error => error);
    }

    fetchStewardEmail() {
        return this.http.fetch('data?key=stewardEmail')
            .then(response => response.json())
            .then(address => {
                return (address.indexOf('\"') > 0) ?
                    address.split('\"')[1] : address;
            })
            .catch(() => '');
    }
}


