import {bindable, customElement} from 'aurelia-framework';
@customElement('patient-count')
export class PatientCount{
    @bindable result;
    @bindable showBreakdown;
    attached() {
        console.log(this.result);
    }
}