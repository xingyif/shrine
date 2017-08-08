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

    //https://shrine-qa2.catalyst:6443/shrine-metadata/qep/queryResults?skip=2&limit=3
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

    fetchQuery(networkId) {//
        return this.http.fetch(`qep/queryResults?networkId=${networkId}`)
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


