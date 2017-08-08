import {inject, bindable, customElement} from 'aurelia-framework';
import {Publisher} from 'common/publisher';
@customElement('error')
export class Error extends Publisher{
    @bindable result;
    constructor(...rest) {
        super(...rest);
    }
}