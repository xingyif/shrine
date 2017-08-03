import {EventAggregator} from 'aurelia-event-aggregator';
import {notifications, commands} from 'common/shrine.messages';
import {QueryStatusModel} from 'common/query-status.model'
export class QueryStatus {
    static inject = [EventAggregator, notifications, commands, QueryStatusModel];
    constructor(evtAgg, notifications, commands, queryStatus) {
        const initialState = () => ({query: {queryName: null, updated: null, complete: false}, nodes: null});
        this.status = initialState();
        // -- publishers --
        const publishFetchNetworkId = n => evtAgg.publish(commands.shrine.fetchNetworkId, n);
        const publishFetchQuery = id => evtAgg.publish(commands.shrine.fetchQuery, id);
        // -- subscribers -- //
        evtAgg.subscribe(notifications.i2b2.queryStarted, (n) => {
            // -- @todo: centralize the logic, investigate adding a new "status" every time -- //
            this.status.query.queryName = n;
            publishFetchNetworkId(n)
        });
        evtAgg.subscribe(notifications.shrine.networkIdReceived, id => publishFetchQuery(id));
        evtAgg.subscribe(notifications.shrine.queryReceived, data => {
            // -- @todo: centralize the logic, investigate adding a new "status" every time -- //
            this.status.query = {...this.status.query, ...data.query};
            this.status.nodes = data.nodes;
            this.status.updated = Number(new Date());
            const complete = data.query.complete;
            const networkId = data.query.networkId;
            if(!complete) {
                publishFetchQuery(networkId)
            }
        });
        // -- testing only -- //
        evtAgg.publish(notifications.i2b2.queryStarted, '@queryqueryName');
    }
}