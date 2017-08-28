/*
https://ilikekillnerds.com/2015/10/all-about-the-aurelia-fetch-client/
http://foreverframe.net/using-interceptors-with-aurelia-fetch-client/
*/

import { HttpClient } from 'aurelia-fetch-client';
import 'fetch';
export class QEPRepository {
    static inject = [HttpClient]
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

    fetchPreviousQueries(limit, skip = 0) {
        return this.http.fetch(`qep/queryResults?limit=${limit}&skip=${skip}`)
            .then(response => response.json())
            .catch(error => error);
    }

    fetchNetworkId(queryName) {
        return this.http.fetch(`qep/networkId?queryName='${queryName}'`)
            .then(response => response.json())
            .catch(error => error);
    }

    fetchQuery(networkId, timeoutSeconds, afterVersion) {//
        return this.http.fetch(`qep/queryResult/${networkId}?timeoutSeconds=${timeoutSeconds}&afterVersion=${afterVersion}`)
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

