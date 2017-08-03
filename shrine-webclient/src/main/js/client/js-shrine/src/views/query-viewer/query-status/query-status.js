import {bindable} from 'aurelia-framework';
import * as _ from 'ramda'
export class QueryStatus {
    static inject = [Math];
    @bindable status;
    constructor(Math) {
        this.floor = Math.floor;
    }
    attached() {
        const svgScaleMultiplier = 75;
        const scaleToSVG = _.curry((f, m, t, n) => f((n / t) * m))
            (this.floor, svgScaleMultiplier, this.status.total);

        //@todo: use composition instead of variables below.
        const status = this.status;
        const finishedPct = scaleToSVG(status.finished);
        const errorPct = scaleToSVG(status.error);
        this.readyOffset = (100 - finishedPct);
        this.errorOffset = (this.readyOffset - errorPct); 
        this.finished = status.finished;
        this.error = status.error;
        this.pending = status.total - (status.finished + status.error);
        this.total = status.total; 
    }
}