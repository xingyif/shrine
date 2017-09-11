import {customElement, bindable} from 'aurelia-framework';
@customElement('node-result')
export class NodeResult{
    @bindable result;
    @bindable queryName;

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