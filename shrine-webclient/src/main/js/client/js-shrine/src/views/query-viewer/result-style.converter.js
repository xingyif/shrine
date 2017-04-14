export class ResultStyleValueConverter {
    toView(value) {
        const result = this.isUnresolved(value) ? 'color:' + this.getColorValue(value) : '';
        return result;
    }

    isUnresolved(value, finishedStatus = 'FINISHED') {
        return !value || value.status !== finishedStatus;
    }

    getColorValue(value, errorStatus = 'ERROR', errorColor = '#FF0000', altColor = '#00FF00') {
        return !value || value.status === errorStatus ? errorColor : altColor;
    }
}
