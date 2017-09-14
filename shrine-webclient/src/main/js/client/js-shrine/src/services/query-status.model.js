import {EventAggregator} from 'aurelia-event-aggregator';
import {QEPRepository} from 'repository/qep.repository';
import {commands, notifications} from './shrine.messages';
export class QueryStatusModel {
    static inject = [EventAggregator, QEPRepository, notifications];
    constructor(evtAgg, qep, notifications) {
        const publishNetworkId = id => evtAgg.publish(notifications.i2b2.networkIdReceived, id);
        const publishQuery = model => evtAgg.publish(notifications.shrine.queryReceived, model);
        const toModel = data => {
            return new Promise((resolve, reject) => {   
                const {results, dataVersion, query: queryData} = data;
                const sort = (a, b) => a.adapterNode.toUpperCase() <= b.adapterNode.toUpperCase()? -1 : 1;
                const nodes = results.length === 0? [] : [...results.sort(sort)];
                const complete = nodes.length > 0 && nodes.filter(n => 'ERROR,COMPLETED,FINISHED'.includes(n.status)).length === nodes.lenth;
                const query = {...queryData, ...{complete: complete}};
                resolve({
                   query,
                   nodes,
                   dataVersion
                });
            });
        };

        //subscribe to fetch network id for query.
        const loadNetworkId = (n) => qep.fetchNetworkId(n)
            .then(result => publishNetworkId(result))
            .catch(error => logError(error));

        const loadQuery = (d) => {           
            qep.fetchQuery(d.networkId, d.timeoutSeconds, d.dataVersion)
            .then(result => toModel(result))
            .catch(error => console.log(`ERROR: ${error}`))
            .then(model => publishQuery(model));
        }
           
        const init = () => {
            evtAgg.subscribe(commands.shrine.fetchQuery, loadQuery);
        }
        init();
    }tt
}