import {EventAggregator} from 'aurelia-event-aggregator';
import { QEPRepository } from 'repository/qep.repository'
import {notifications} from './shrine.messages';
export class QueriesModel {
    static inject = [EventAggregator, QEPRepository,  notifications];
    constructor(evtAgg, QEPRepository, notifications) {
        const qep = QEPRepository;
        const maxQueriesPerFetch = 40;
        let loadedCount = 0;
        let totalQueries = 0;
        let data = null;

        QueriesModel.prototype.load = () => 
            qep.fetchPreviousQueries(this.maxQueriesPerFetch() + this.loadedCount())
                .then(result => {
                    totalQueries = result.rowCount;
                    loadedCount = result.queryResults.length
                    return result;
                })
                .catch(error => console.log(error))
                .then(toPages)
                .then(pages => {
                    data = pages;
                    evtAgg.publish(notifications.shrine.queriesReceived, data);
                });
        
        QueriesModel.prototype.totalQueries = () => totalQueries;
        QueriesModel.prototype.loadedCount = () => loadedCount;
        QueriesModel.prototype.maxQueriesPerFetch = () => maxQueriesPerFetch;
        QueriesModel.prototype.moreToLoad = () => loadedCount < totalQueries;
        QueriesModel.prototype.hasData = () => data !== null && data !== undefined;

        const toPages = (data, nodesPerPage = 6) => {
            return new Promise((resolve, reject) => {
                const pages = [];
                /*@todo: move to config*/
                const nodesPerPage = 6;
                const nodes = data.adapters;
                const lastNodeIndex = nodes.length;
                const queries = data.queryResults;

                for (let i = 0; i < lastNodeIndex; i = i + nodesPerPage) {
                    const numberOfNodes = geNumberOfNodes(nodes, i, nodesPerPage);
                    const pageNodes = nodes.slice(i, numberOfNodes);
                    const results = mapQueries(pageNodes, queries);
                    pages.push({
                        nodes: pageNodes,
                        results: results
                    })
                }

                resolve(pages);
            });
        }

        const geNumberOfNodes = (nodes, startIndex, nodesPerPage) => {
            const numNodes = startIndex + nodesPerPage;
            return numNodes < nodes.length ? numNodes : nodes.length;
        }

        const mapQueries = (nodes, queries) => {
            const results = [];
            queries.forEach((q, i) => {
                const result = {
                    name: q.query.queryName,
                    id: q.query.networkId,
                    date: q.query.dateCreated,
                    flagged: q.query.flagged === true,
                    flagMessage: q.query.flagMessage || null,
                    nodeResults: [],
                    status: q.adaptersToResults.reduce((s, r) => {
                        const finished = r.status === "FINISHED"? s.finished + 1 : s.finished;
                        const error = r.status === "ERROR"? s.error + 1 : s.error; 
                        return {error: error, finished: finished, total: q.adaptersToResults.length}
                    }, {error: 0, finished: 0, total: q.adaptersToResults.length})
                };
                nodes.forEach(n => {
                    result.nodeResults.push(q.adaptersToResults.find(a => a.adapterNode === n));
                });
                results.push(result);
            })

            return results;
        }
    }
}