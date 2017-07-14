import { bindable, inject } from 'aurelia-framework';


export class QueryStatus {
    @bindable status;
    attached() {
        const status = this.status;
        const scaleToSVG = (n, t) => Math.floor((n / t) * 75);
        const finishedPct = scaleToSVG(status.finished, status.total);
        const errorPct = scaleToSVG(status.error, status.total);
        this.readyOffset = (100 - finishedPct);
        this.errorOffset = (this.readyOffset - errorPct); 
        this.finished = status.finished;
        this.error = status.error;
        this.pending = status.total - (status.finished + status.error);
        this.total = status.total; 
    }
}