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
            this.status.updated = Number(new Date());
            this.nodes = initialState().nodes;
            this.hubMsg = initialState().hubMsg;
            this.status.query.queryName = n;
        });

        this.subscribe(this.notifications.i2b2.networkIdReceived, d => {
            const runningPreviousQuery = this.status === undefined;
            if(runningPreviousQuery) this.status = initialState().status;
            const {networkId, name = this.status.queryName} = d;
            this.status.query.networkId = networkId;
            this.status.query.queryName = name;
            this.status.updated = Number(new Date());
            this.nodes = initialState().nodes;
            this.hubMsg = hubMsgTypes.RESPONSE_RECEIVED;
            this.publish(this.commands.shrine.fetchQuery, {networkId, timeoutSeconds: TIMEOUT_SECONDS, dataVersion: DEFAULT_VERSION})
        });

        this.subscribe(this.notifications.i2b2.exportQuery, () => {
            const nodes = this.nodes;
            this.publish(this.commands.shrine.exportResult, {nodes});
        })

        this.subscribe(this.notifications.i2b2.clearQuery, () => {
            this.nodes = initialState().nodes;
            this.status =  initialState().status;
        })
        this.subscribe(this.notifications.shrine.queryReceived, data => {
            const {query, nodes, dataVersion = DEFAULT_VERSION,query: {networkId = null}} = data; 
            const {complete = false} = query; 
            const timeoutSeconds = TIMEOUT_SECONDS;
            if(networkId !== this.status.query.networkId) return;
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
const hubMsgTypes = {
    WAITING_ON_RESPONSE: 'Waiting on response from network',
    RESPONSE_RECEIVED: 'Response received from network.  Waiting on results'
}
const initialState = (n) => ({status: { query: { networkId: null, queryName: null, updated: null, complete: false}}, nodes: [], hubMsg: hubMsgTypes.WAITING_ON_RESPONSE});
