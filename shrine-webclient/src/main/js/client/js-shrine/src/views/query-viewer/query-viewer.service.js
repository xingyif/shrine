import { inject } from 'aurelia-framework';
import { HttpClient } from 'aurelia-fetch-client';
import 'fetch';

const nodesPerScreen = 10;

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

    getNodes(queries) {
        return (queries.length > 0) ?
            queries[0].results.map(result => result.node) : [];
    }

    getScreens(nodes, queries) {
        return new Promise((resolve, reject) => {
            const lastNodeIndex = nodes.length;
            let screens = [];

            for (let i = 0; i < lastNodeIndex; i = i + nodesPerScreen) {
                const endIndex = (i + nodesPerScreen < lastNodeIndex) ? i + nodesPerScreen : lastNodeIndex - 1;
                const screenId = String(nodes[i]).substr(0, 1) + '-' + String(nodes[endIndex]).substr(0, 1);
                const screenNodes = nodes.slice(i, endIndex);
                const screenQueries = queries.map(query => {
                    return {
                        id: query.id,
                        name: query.name,
                        results: query.results.slice(i, endIndex)
                    };
                });

                screens.push({
                    name: screenId,
                    nodes: screenNodes,
                    queries: screenQueries
                });
            }

            resolve(screens);
        });
    }
}