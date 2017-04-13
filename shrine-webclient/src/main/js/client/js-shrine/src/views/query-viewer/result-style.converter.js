export class ResultStyleValueConverter {
    toView(value) {
        const result = isUnresolved(value) ? 'color:' + getColorValue(value) : '';
        return result;
    }
}

// -- @todo: move to service for unit testing --//
function isUnresolved(value, finishedStatus = 'FINISHED') {
    return !value || value.status !== finishedStatus;
}

// -- @todo: move to service for unit testing --//
function getColorValue(value, errorStatus = 'ERROR', errorColor = '#FF0000', altColor = '#00FF00') {
    return !value || value.status === errorStatus ? errorColor : altColor;
}
