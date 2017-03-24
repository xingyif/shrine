import {inject} from 'aurelia-framework';
import {HttpClient} from 'aurelia-fetch-client';
import 'fetch';

@inject(HttpClient)
export class QueryViewerService {

    constructor(http) {
        http.configure(config => {
            config
                .useStandardConfiguration()
                .withBaseUrl(this.url);
        });

        this.http = http;
    }

    fetchPreviousQueries() {
        return this.http.fetch('previous-queries')
            .then(response => response.json())
            .catch(error => error);
    }

    get url() {
        const port = '8000';
        const url = document.URL;
        const service = '6443/shrine-proxy/request/shrine/api/';
        return url.substring(0, url.indexOf(port)) + service;
    }
}