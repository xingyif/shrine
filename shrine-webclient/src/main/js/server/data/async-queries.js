const institutions = require('./institutions').sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));

module.exports = {
    getQueryResults: getQueryResults
};

function getQueryResults(numberOfResults = 100, numberOfInstitutions = 100) {
    let queryResults = [];
    const results = getInstutionResults(numberOfInstitutions);
    for (let i = 0; i < numberOfResults; i++) {
        let query = {
            id: i,
            name: 'Query' + i,
            results: results
        }
        queryResults.push(query);
    }

    return {
        'queries': queryResults
    };
}

function getInstutionResults(numberOfInstitutions = 100, resultBase = 10000000) {
    const length = numberOfInstitutions < institutions.length ? numberOfInstitutions : institutions.length;
    const resultSet = [];
    for (let i = 0; i < length; i++) {
        const institution = institutions[i];
        const result = resultBase + i;

        resultSet.push(getInstitutionResult(
            institution, resultBase, i,
            ind => ind % 9 === 0,
            ind => ind % 4 === 0));
    }

    return resultSet;
}


function getInstitutionResult(institution, resultBase, index, isErrorFn, isPendingFn) {

    let result = {
        node: institution,
        result: resultBase + index
    };

    if (isErrorFn(index)) {
        result.result =  'ERROR';
    }

    else if (isPendingFn(index)) {
        result.result  = 'PENDING';
    }

    console.log(result);

    return result;
}



