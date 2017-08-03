import {inject, bindable} from 'aurelia-framework';
import {Publisher} from 'common/publisher';
export class Error extends Publisher{
    @bindable result;
    constructor(...rest) {
        super(...rest);
    }
}