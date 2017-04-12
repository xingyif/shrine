import { inject } from 'aurelia-framework';
import {QEPRepository} from 'repository/qep.repository';
import {QueryViewerConfig} from './query-viewer.config';

@inject(QEPRepository, QueryViewerConfig)
export class QueryViewerService {
    constructor(repository, config) {
        this.repository = repository;
        this.config = config;
    }

    fetchPreviousQueries() {
        return this.repository.fetchPreviousQueries();
    }

    getScreens(nodes, queries) {
        return new Promise((resolve, reject) => {
            const lastNodeIndex = nodes.sort().length;
            const screens = [];
            for (let i = 0; i < lastNodeIndex; i = i + this.config.maxNodesPerScreen) {
                const numberOfNodesOnScreen = this.getNumberOfNodesOnScreen(nodes, i, this.config.maxNodesPerScreen);
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
        const numNodes = startIndex + this.config.maxNodesPerScreen;
        return numNodes < nodes.length ? numNodes : nodes.length;
    }

    getScreenId(nodes, start, end) {
        const startNode = nodes[start];
        const endNode = nodes[end];
        return String(startNode).substr(0, 1) + '-' + String(endNode).substr(0, 1);
    }
}