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
        this.component = './status-msg.html';
        if(status === "ERROR") {
            this.component = './error.html';
        }
        else if(['COMPLETED', 'FINISHED'].indexOf(status) > -1) {
            this.component = './patient-count.html';
        }
    }
}