import {bindable} from 'aurelia-framework'
export class Error{
    @bindable result;
    
    constructor() {
        Error.prototype.publishError = data => console.log(data);
    }
    
    attached() {
        
    }
}