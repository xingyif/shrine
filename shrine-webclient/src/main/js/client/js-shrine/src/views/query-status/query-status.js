import { customElement, observable} from 'aurelia-framework';
import { QueryStatusModel } from 'services/query-status.model';
import { PubSub } from 'services/pub-sub';
@customElement('query-status')
export class QueryStatus extends PubSub {
    @observable status
    static inject = [QueryStatusModel];
    constructor(queryStatus, ...rest) {
        super(...rest);
        me.set(this, {
            isDevEnv: document.location.href.includes('http://localhost:8000/'),
            exportAvailable: false
        });
    }
    statusChanged(newValue, oldValue) {
        if(!newValue.nodes || !newValue.nodes.length) {
            me.get(this).exportAvailable = false;
            this.publish(this.notifications.shrine.queryUnavailable);
            return;
        }
        me.get(this).exportAvailable = true;
        this.publish(this.notifications.shrine.queryAvailable);
    }
    attached() {
        // -- subscribers -- //
        this.subscribe(this.notifications.i2b2.queryStarted, (n) => {
            this.status = initialState();
            this.status.query.queryName = n;
        });

        this.subscribe(this.notifications.i2b2.networkIdReceived, d => {
            const {networkId, name} = d;
            const state = initialState();
            const {nodes} = state;
            state.query.queryName = name || state.query.queryName; 
            this.status = this.status? {...this.status, ...{nodes}} : state;
            this.publish(this.commands.shrine.fetchQuery, {networkId, timeoutSeconds: TIMEOUT_SECONDS, dataVersion: DEFAULT_VERSION})
        });

        this.subscribe(this.notifications.i2b2.exportQuery, () => {
            this.publish(this.commands.shrine.exportResult, { ...{}, ...this.status });
        })

        this.subscribe(this.notifications.i2b2.clearQuery, () => {
            this.status = initialState();
        })
        this.subscribe(this.notifications.shrine.queryReceived, data => {
            const {query, nodes, dataVersion, complete, query:{networkId}} = data; 
            const updated = Number(new Date());
            this.status = { ...this.status, ...{ query, nodes, updated } }
            if (!complete) {
                this.publish(this.commands.shrine.fetchQuery, {networkId, dataVersion, TIMEOUT_SECONDS});
            }
        });

        if (me.get(this).isDevEnv) {
            //this.publish(this.notifications.i2b2.queryStarted, "started query");
            this.publish(this.notifications.i2b2.networkIdReceived, {networkId: 1, name: "started query"});
        }
    }
}
const TIMEOUT_SECONDS = 15;
const DEFAULT_VERSION = -1;
const me = new WeakMap();
const initialState = (n) => ({ query: { queryName: null, updated: null, complete: false }, nodes: null });
