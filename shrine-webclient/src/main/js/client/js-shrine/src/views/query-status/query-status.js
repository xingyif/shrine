import { QueryStatusModel } from 'services/query-status.model';
import { PubSub } from 'services/pub-sub'
export class QueryStatus extends PubSub {
    static inject = [QueryStatusModel];
    constructor(queryStatus, ...rest) {
        super(...rest);
        const initialState = () => ({ query: { queryName: null, updated: null, complete: false }, nodes: null });
        this.status = initialState();
        // -- subscribers -- //
        this.subscribe(this.notifications.i2b2.queryStarted, (n) => {
            // -- @todo: centralize the logic, investigate adding a new "status" every time -- //
            this.status.query.queryName = n;
        });
        this.subscribe(this.notifications.i2b2.networkIdReceived, id => this.publish(this.commands.shrine.fetchQuery, id));
        this.subscribe(this.notifications.shrine.queryReceived, data => {    
            const query = data.query;
            const nodes = data.nodes;
            const updated = Number(new Date());
            const complete = data.query.complete;
            const networkId = data.query.networkId;
            this.status = {...this.status, ...{query, nodes, updated}}
            if (!complete) {
                window.setTimeout(() => this.publish(this.commands.shrine.fetchQuery, networkId), 10000);
            }
        });

        const isDevEnv = document.location.href.includes('http://localhost:8000/');
        if (isDevEnv) {
            this.publish(this.notifications.i2b2.queryStarted, "started query");
            this.publish(this.notifications.i2b2.networkIdReceived, 1);
        }
    }
}