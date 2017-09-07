import {inject, bindable, customElement} from 'aurelia-framework';
import {PubSub} from 'services/pub-sub';
@customElement('error')
export class Error extends PubSub{
    @bindable result;
    constructor(...rest) {
        super(...rest);
    }

    attached() {
        console.log(this.result);
    }
}