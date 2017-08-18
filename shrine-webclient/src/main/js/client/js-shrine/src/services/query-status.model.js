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
                const finishedCount = nodes.reduce((s, r) => ['FINISHED', 'ERROR'].indexOf(r.status) != -1? s + 1 : s, 0);
                const query = {...data.query, ...{complete: nodes.length > 0 && nodes.length === finishedCount}};
                resolve({
                   query,
                   nodes,
                   finishedCount
                });
            });
        };

        //subscribe to fetch network id for query.
        const loadNetworkId = (n) => qep.fetchNetworkId(n)
            .then(result => publishNetworkId(result))
            .catch(error => logError(error));

        const loadQuery = (d) => {
            return qep.fetchQuery(d.id, d.timeoutSeconds, d.afterVersion)
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