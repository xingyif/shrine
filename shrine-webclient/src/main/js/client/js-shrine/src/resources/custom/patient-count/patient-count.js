import {bindable, customElement} from 'aurelia-framework';
@customElement('patient-count')
export class PatientCount{
    @bindable result;
    attached() {
        console.log(this.result)
    }
}