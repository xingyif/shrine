import { customElement } from 'aurelia-framework';
import { QueryStatusModel } from 'services/query-status.model';
import { PubSub } from 'services/pub-sub';
@customElement('query-status')
export class QueryStatus extends PubSub {
    static inject = [QueryStatusModel];
    constructor(queryStatus, ...rest) {
        super(...rest);
        privateProps.set(this, {
            isDevEnv: document.location.href.includes('http://localhost:8000/')
        });
    }
    attached() {
        // -- subscribers -- //
        this.subscribe(this.notifications.i2b2.queryStarted, (n) => {
            this.status = initialState();
            this.status.query.queryName = n;
        });
        this.subscribe(this.notifications.i2b2.networkIdReceived, id => {
            this.publish(this.commands.shrine.fetchQuery, {id, timeoutSeconds, dataVersion: defaultVersion})
        });
        this.subscribe(this.notifications.shrine.queryReceived, data => {
            const query = data.query;
            const nodes = data.nodes;
            const dataVersion= data.dataVersion;
            const updated = Number(new Date());
            const complete = data.query.complete;
            const id = data.query.networkId;
            this.status = { ...this.status, ...{ query, nodes, updated } }
            if (!complete) {
                window.setTimeout(() => this.publish(this.commands.shrine.fetchQuery, {id, dataVersion, timeoutSeconds}), 5000);
            }
            else {
                this.publish(this.commands.shrine.exportResult, { ...{}, ...this.status });
            }
        });

        if (privateProps.get(this).isDevEnv) {
            this.publish(this.notifications.i2b2.queryStarted, "started query");
            this.publish(this.notifications.i2b2.networkIdReceived, 1);
        }
    }
}
const timeoutSeconds = 15;
const defaultVersion = -1;
const privateProps = new WeakMap();
const initialState = () => ({ query: { queryName: null, updated: null, complete: false }, nodes: null });