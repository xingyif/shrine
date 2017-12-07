import {customElement, bindable} from 'aurelia-framework';
import {PubSub} from 'services/pub-sub';
@customElement('node-result')
export class NodeResult extends PubSub{
    @bindable result;
    @bindable queryName;

    constructor(...rest) {
        super(...rest);
    }

    attached() {
        const status = this.result.status;
        this.component = './submitted.html';
        if(status === "ERROR") {
            this.component = './error.html';
        }
        else if('COMPLETED,FINISHED'.includes(status)) {
            this.component = './patient-count.html';
        }
        else if('PROCESSING,INCOMPLETE,COMPLETE'.includes(status)) {
            this.component = './status.html';
        }
        else if(status.toLowerCase().includes('queue')) {
            this.component = './queued.html';
        }
    }
}