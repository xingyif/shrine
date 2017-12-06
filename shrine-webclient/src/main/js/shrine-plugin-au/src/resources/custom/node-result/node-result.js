import {PLATFORM} from 'aurelia-pal';
import {customElement, bindable} from 'aurelia-framework';
import {PubSub} from 'services/pub-sub';
@customElement('node-result')
@bindable('result')
@bindable('queryName')
export class NodeResult extends PubSub{
    constructor(...rest) {
        super(...rest);
    }

    attached() {
        const status = this.result.status;
        this.component = PLATFORM.moduleName('./submitted.html');
        if(status === "ERROR") {
            this.component = PLATFORM.moduleName('./error.html');
        }
        else if('COMPLETED,FINISHED'.includes(status)) {
            this.component = PLATFORM.moduleName('./patient-count.html');
        }
        else if('PROCESSING,INCOMPLETE,COMPLETE'.includes(status)) {
            this.component = PLATFORM.moduleName('./status.html');
        }
        else if(status.toLowerCase().includes('queue')) {
            this.component = PLATFORM.moduleName('./queued.html');
        }
    }
}