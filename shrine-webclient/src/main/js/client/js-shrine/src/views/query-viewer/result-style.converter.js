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


/*
Object {startDate: 1490984964085, count: -1, networkQueryId: 7768019175359373000, statusMessage: "FINISHED", changeDate: 1490984965845…}
adapterNode
:
"shrine-qa3"
changeDate
:
1490984965845
count
:
-1
endDate
:
1490984964917
instanceId
:
1899
networkQueryId
:
7768019175359373000
resultId
:
2181
resultType
:
Object
startDate
:
1490984964085
status
:
"FINISHED"
statusMessage
:
"FINISHED"
*/