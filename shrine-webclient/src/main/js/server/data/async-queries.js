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
    let resultSet = [];
    for (let i = 0; i < length; i++) {
        const institution = institutions[i];
        console.log(institution);
        const result = resultBase + i;
        resultSet.push({
            node: institution,
            result: result
        });
    }

    return resultSet;
}

