const institutions = require('./institutions').sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));

module.exports = {
    getQueryResults: getQueryResults
};

function getQueryResults (skip=0, numberOfQueries = 100, numberOfNodes = 100, rowCount = 500){
    const length = numberOfNodes < institutions.length ? numberOfNodes: institutions.length;
    const queryResults = [];
    const adapters = institutions.slice(0, length);
    for(let i =0;  i < numberOfQueries; i ++) {
         queryResults.push({
            "query": {
                "expression": "Term(\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Female\\)",
                "queryName": `${i} Female@19:46:33`,
                "deleted": false,
                "changeDate": 1490571996396,
                "userName": "ij22",
                "networkId": (2226563841174084000 + i),
                "queryXml": "<runQuery><projectId>SHRINE</projectId><waitTimeMs>180000</waitTimeMs><authenticationInfo><domain>i2b2demo</domain><username>ij22</username><credential isToken=\"true\">SessionKey:Gv6xaK4AQNjQipmypc7p</credential></authenticationInfo><queryId>2226563841174084953</queryId><topicId>3</topicId><topicName>medication only_27th march</topicName><outputTypes><resultType><name>PATIENT_AGE_COUNT_XML</name><isBreakdown>true</isBreakdown><description>Age patient breakdown</description><displayType>CATNUM</displayType></resultType><resultType><id>4</id><name>PATIENT_COUNT_XML</name><isBreakdown>false</isBreakdown><description>Number of patients</description><displayType>CATNUM</displayType></resultType><resultType><name>PATIENT_GENDER_COUNT_XML</name><isBreakdown>true</isBreakdown><description>Gender patient breakdown</description><displayType>CATNUM</displayType></resultType><resultType><name>PATIENT_RACE_COUNT_XML</name><isBreakdown>true</isBreakdown><description>Race patient breakdown</description><displayType>CATNUM</displayType></resultType><resultType><name>PATIENT_VITALSTATUS_COUNT_XML</name><isBreakdown>true</isBreakdown><description>Vital Status patient breakdown</description><displayType>CATNUM</displayType></resultType></outputTypes><queryDefinition><name>Female@19:46:33</name><expr><term>\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Female\\</term></expr></queryDefinition></runQuery>",
                "dateCreated": 1490571996395,
                "userDomain": "i2b2demo"
            },
            "adaptersToResults": getInstutionResults(adapters)
        });
    }

    return {
        "adapters": adapters,
        "queryResults": queryResults,
        "flags": {},
        "rowCount": rowCount
    };
}


const getInstutionResults = (adapters) => {
    const adaptersToResults = [];
    const status = i => i % 7 === 0? 'ERROR' : 'FINISHED';
    adapters.forEach((e, i) => {
        adaptersToResults.push({
            "startDate": 1490984964085,
            "count": i,
            "networkQueryId": (7768019175359370000 + i),
            "statusMessage": status(i),
            "changeDate": 1490984965845,
            "instanceId": (1000 + i),
            "resultId": (2000 + i),
            "status": status(i),
            "endDate": 1490984964917,
            "adapterNode": e,
            "resultType": {
                "isBreakdown": false,
                "name": "PATIENT_COUNT_XML",
                "id": 4,
                "i2b2Options": {
                    "description": "Number of patients",
                    "displayType": "CATNUM"
                }
            }
        });

    });

    return adaptersToResults;
}




