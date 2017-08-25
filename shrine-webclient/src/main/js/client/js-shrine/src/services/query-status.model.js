import {EventAggregator} from 'aurelia-event-aggregator';
import {QEPRepository} from 'repository/qep.repository';
import {commands, notifications} from './shrine.messages';
export class QueryStatusModel {
    static inject = [EventAggregator, QEPRepository, notifications];
    constructor(evtAgg, qep, notifications) {
        const publishNetworkId = id => evtAgg.publish(notifications.i2b2.networkIdReceived, id);
        const publishQuery = model => evtAgg.publish(notifications.shrine.queryReceived, model);
        const logError = error => console.log(`ERROR: ${error}`);
        const toModel = data => {
            return new Promise((resolve, reject) => {                
                const nodes = [...data.results];
                const dataVersion = data.dataVersion;
                const complete = nodes.length > 0 && nodes.filter(n => 'ERROR,COMPLETED,FINISHED'.includes(n.status)).length === nodes.length;
                const query = {...data.query, ...{complete: complete}};
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
            return qep.fetchQuery(d.id, d.timeoutSeconds, d.dataVersion)
            .then(result => toModel(result))
            .catch(error => logError(error))
            .then(model => publishQuery(model));
        }
           
            
        const init = () => {
            evtAgg.subscribe(commands.shrine.fetchQuery, loadQuery);
        }
        init();
    }
}