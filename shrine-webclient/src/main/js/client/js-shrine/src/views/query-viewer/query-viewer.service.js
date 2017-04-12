import { inject } from 'aurelia-framework';
import { HttpClient } from 'aurelia-fetch-client';
import 'fetch';

const maxNodesPerScreen = 10;

@inject(HttpClient, 'shrine')
export class QueryViewerService {
    constructor(http, shrine) {
        if (http !== undefined) {
            http.configure(config => {
                config
                    .useStandardConfiguration()
                    .withBaseUrl(this.url)
                    .withDefaults({
                        headers: {
                            'Authorization': 'Basic ' + shrine.auth
                        }
                    });
            });

            this.http = http;
        }
    }

    fetchPreviousQueries() {
        return this.http.fetch('qep/queryResults')
            .then(response => response.json())
            .catch(error => error);
    }

    get url() {
        const port = '6443';
        const url = document.URL;
        const service = '/shrine-metadata/';
        // https://shrine-qa2.catalyst:6443/shrine-metadata/qep/queryResults
        //const service = '6443/shrine-proxy/request/shrine/api/';
        return url.substring(0, url.indexOf(port) + port.length) + service;
    }

    getScreens(nodes, queries) {
        return new Promise((resolve, reject) => {
            const lastNodeIndex = nodes.sort().length;
            const screens = [];
            for (let i = 0; i < lastNodeIndex; i = i + maxNodesPerScreen) {
                const numberOfNodesOnScreen = this.getNumberOfNodesOnScreen(nodes, i, maxNodesPerScreen);
                const endIndex = numberOfNodesOnScreen - 1;
                const screenId = this.getScreenId(nodes, i, endIndex);
                const screenNodes = nodes.slice(i, numberOfNodesOnScreen);
                const screenNodesToQueriesMap = this.mapQueriesToScreenNodes(screenNodes, queries, this.findQueriesForNode);
                screens.push({
                    id: screenId,
                    nodes: screenNodes, 
                    results: screenNodesToQueriesMap
                });
            }
            resolve(screens);
        });
    }

    mapQueriesToScreenNodes(nodes, queries) {
        const results = [];
        queries.forEach( (q, i) => {
            const result = {
                name: q.query.queryName,
                id: q.query.networkId,
                nodeResults: []
            };
            nodes.forEach(n => {
                result.nodeResults.push(q.adaptersToResults.find( a => a.adapterNode === n));
            });
            results.push(result);
        });
        return results;
    }

    getNumberOfNodesOnScreen(nodes, startIndex) {
        const numNodes = startIndex + maxNodesPerScreen;
        return numNodes < nodes.length ? numNodes : nodes.length;
    }

    getScreenId(nodes, start, end) {
        const startNode = nodes[start];
        const endNode = nodes[end];
        return String(startNode).substr(0, 1) + '-' + String(endNode).substr(0, 1);
    }
}