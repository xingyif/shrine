import {QueryStatusModel} from 'services/query-status.model';
import {PubSub} from 'services/pub-sub'
export class QueryStatus extends PubSub{
    static inject = [QueryStatusModel];
    constructor(queryStatus, ...rest) {
        super(...rest);
        const initialState = () => ({query: {queryName: null, updated: null, complete: false}, nodes: null});
        this.status = initialState();
        // -- subscribers -- //
        this.subscribe(this.notifications.i2b2.queryStarted, (n) => {
            // -- @todo: centralize the logic, investigate adding a new "status" every time -- //
            this.status.query.queryName = n;
        });
        this.subscribe(this.notifications.i2b2.networkIdReceived, id => this.publish(this.commands.shrine.fetchQuery, id));
        this.subscribe(this.notifications.shrine.queryReceived, data => {
            // -- @todo: centralize the logic, investigate adding a new "status" every time -- //
            this.status.query = {...this.status.query, ...data.query};
            this.status.nodes = data.nodes;
            this.status.updated = Number(new Date());
            const complete = data.query.complete;
            const networkId = data.query.networkId;
            if(!complete) {
                window.setTimeout(() => publishFetchQuery(networkId), 10000);
            }
        });
        
        // -- for testing only -- //
        
        this.publish(this.notifications.i2b2.queryStarted, "started query");
        this.publish(this.notifications.i2b2.networkIdReceived, 1);
        
    }
}