import { customElement, observable} from 'aurelia-framework';
import { QueryStatusModel } from 'services/query-status.model';
import { PubSub } from 'services/pub-sub';
@customElement('query-status')
export class QueryStatus extends PubSub {
    @observable nodes
    static inject = [QueryStatusModel];
    constructor(queryStatus, ...rest) {
        super(...rest);
        me.set(this, {
            isDevEnv: document.location.href.includes('http://localhost:8000/'),
            exportAvailable: false
        });
    }
    nodesChanged(newValue, oldValue) {
        if(!newValue || !newValue.length) {
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
            this.status = initialState().status;
            this.nodes = initialState().nodes;
            this.status.query.queryName = n;
        });

        this.subscribe(this.notifications.i2b2.networkIdReceived, d => {
            if(this.status && this.status.canceled) return;
            const {networkId} = d;
            this.status.query.networkId = networkId;
            this.nodes = initialState().nodes;
            this.publish(this.commands.shrine.fetchQuery, {networkId, timeoutSeconds: TIMEOUT_SECONDS, dataVersion: DEFAULT_VERSION})
        });

        this.subscribe(this.notifications.i2b2.exportQuery, () => {
            const nodes = this.nodes;
            this.publish(this.commands.shrine.exportResult, {nodes});
        })

        this.subscribe(this.notifications.i2b2.clearQuery, () => {
            this.status = {...initialState(), ...{canceled: true}};
        })
        this.subscribe(this.notifications.shrine.queryReceived, data => {
            const {query, nodes, dataVersion = DEFAULT_VERSION, complete, query:{networkId}} = data; 
            const timeoutSeconds = TIMEOUT_SECONDS;
            if(networkId !== this.status.query.networkId || this.status.canceled) return;
            const updated = Number(new Date());
            Object.assign(this.status, {query, updated});
            this.nodes = nodes;
            if (!complete) {
                this.publish(this.commands.shrine.fetchQuery, {networkId, dataVersion, timeoutSeconds});
            }
        });

        if (me.get(this).isDevEnv) {
            this.publish(this.notifications.i2b2.queryStarted, "started query");
            window.setTimeout(() => 
                this.publish(this.notifications.i2b2.networkIdReceived, {networkId: '2421519216383772161', name: "started query"}), 2000);
        }
    }
}
const TIMEOUT_SECONDS = 15;
const DEFAULT_VERSION = -1;
const me = new WeakMap();
const initialState = (n) => ({status: { query: { networkId: null, queryName: null, updated: null, complete: false, canceled: false}}, nodes: [] });
