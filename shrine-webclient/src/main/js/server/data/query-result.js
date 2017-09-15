module.exports = {
    "results": [
        {
            "count": 0,
            "networkQueryId": 2421519216383772200,
            "statusMessage": "No results available",
            statusDescription: undefined,
            "changeDate": 1501001608958,
            "instanceId": 0,
            "resultId": 0,
            "status": "ERROR",
            "adapterNode": "shrine-dev1",
            "breakdowns": [
            ],
            "problemDigest": {
                "codec":"net.shrine.adapter.QueryNotFound",
                "description":"No query with id 8252740983617941467 found on shrine-dev1.catalyst",
                 "detailsString":"line1,lin2,line3",
                "epoch":0,
                "stampText":"Wed Sep 06 12:45:32 EDT 2017 on shrine-dev1.catalyst Adapter",
                "summary":"Query not found"
            }
        },
        {
            "count": 0,
            "networkQueryId": 2421519216383772200,
            "statusMessage": "No results available",
            statusDescription: undefined,
            "changeDate": 1501001608958,
            "instanceId": 0,
            "resultId": 0,
            "status": "ERROR",
            "adapterNode": "shrine-dev2",
            "breakdowns": [
            ],
            "problemDigest": {
                "codec":"net.shrine.adapter.QueryNotFound",
                "description":"No query with id 8252740983617941467 found on shrine-dev2.catalyst",
                 "detailsString":"line1,lin2,line3",
                "epoch":0,
                "stampText":"Wed Sep 06 12:45:32 EDT 2017 on shrine-dev2.catalyst Adapter",
                "summary":"Query not found"
            }
        },

        {
            "count": 1795,
            "networkQueryId": 2421519216383772200,
            "statusMessage": "HUB_WILL_SUBMIT",
            "changeDate": 1501001608966,
            "instanceId": 174,
            "resultId": 320,
            "status": "HUB_WILL_SUBMIT",
            "resultType": {
                "isBreakdown": false,
                "name": "PATIENT_COUNT_XML",
                "id": 4,
                "i2b2Options": {
                    "description": "Number of patients",
                    "displayType": "CATNUM"
                }
            },
            "adapterNode": "shrine-dev4"
        },
        {
            "count": 1795,
            "networkQueryId": 2421519216383772200,
            "statusMessage": "SMALL_QUEUE",
            "changeDate": 1501001608966,
            "instanceId": 174,
            "resultId": 320,
            "status": "SMALL_QUEUE",
            "resultType": {
                "isBreakdown": false,
                "name": "PATIENT_COUNT_XML",
                "id": 4,
                "i2b2Options": {
                    "description": "Number of patients",
                    "displayType": "CATNUM"
                }
            },
            "adapterNode": "shrine-dev5"
        },
        {
            "count": 1795,
            "networkQueryId": 2421519216383772200,
            "statusMessage": "PROCESSING",
            "changeDate": 1501001608966,
            "instanceId": 174,
            "resultId": 320,
            "status": "PROCESSING",
            "resultType": {
                "isBreakdown": false,
                "name": "PATIENT_COUNT_XML",
                "id": 4,
                "i2b2Options": {
                    "description": "Number of patients",
                    "displayType": "CATNUM"
                }
            },
            "adapterNode": "shrine-dev6"
        },
        {
            "count": 1795,
            "networkQueryId": 2421519216383772200,
            "statusMessage": "FINISHED",
            "changeDate": 1501001608966,
            "instanceId": 174,
            "resultId": 320,
            "status": "FINISHED",
            "resultType": {
                "isBreakdown": false,
                "name": "PATIENT_COUNT_XML",
                "id": 4,
                "i2b2Options": {
                    "description": "Number of patients",
                    "displayType": "CATNUM"
                }
            },
            "adapterNode": "shrine-dev3",
            "breakdowns": [
                {
                    resultType: {
                        i2b2Options: {
                            description: "Race Patient Breakdown",
                            displayType: "CATNUM"
                        },
                        isBreakdown: true,
                        name: "PATIENT_RACE_COUNT_XML",
                    },
                    "results": [
                        { changeDate: 1502294526455, value: -1, dataKey: "Other" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Asian Pacific Islander" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Middle Eastern" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Not recorded" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Eskimo" },
                        { changeDate: 1502294526455, value: -1, dataKey: "American Indian" },
                        { changeDate: 1502294526455, value: 80, dataKey: "White" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Native American" },
                        { changeDate: 1502294526455, value: 370, dataKey: "Black" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Multiracial" },
                        { changeDate: 1502294526455, value: 170, dataKey: "Hispanic" },
                        { changeDate: 1502294526455, value: 95, dataKey: "Indian" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Aleutian" },
                        { changeDate: 1502294526455, value: 105, dataKey: "Asian" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Oriental" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Navajo" }
                    ]
                },
                {
                    resultType: {
                        i2b2Options: {
                            description: "Age patient breakdown",
                            displayType: "CATNUM"
                        },
                        isBreakdown: true,
                        name: "PATIENT_AGE_COUNT_XML",
                    },
                    results: [
                        { changeDate: 1502294526455, value: 290, dataKey: "  18-34 years old" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Not recorded" },
                        { changeDate: 1502294526455, value: 80, dataKey: "  45-54 years old" },
                        { changeDate: 1502294526455, value: -1, dataKey: "  0-9 years old" },
                        { changeDate: 1502294526455, value: 235, dataKey: "  35-44 years old" },
                        { changeDate: 1502294526455, value: 45, dataKey: "  75-84 years old" },
                        { changeDate: 1502294526455, value: 35, dataKey: ">= 85 years old" },
                        { changeDate: 1502294526455, value: 65, dataKey: "  65-74 years old" },
                        { changeDate: 1502294526455, value: 150, dataKey: ">= 65 years old" },
                        { changeDate: 1502294526455, value: -1, dataKey: "  10-17 years old" },
                        { changeDate: 1502294526455, value: 45, dataKey: "  55-64 years old" }
                    ]
                },
                {
                    resultType: {
                        i2b2Options: {
                            description: "Gender patient breakdown",
                            displayType: "CATNUM"
                        },
                        isBreakdown: true,
                        name: "PATIENT_COUNT_COUNT_XML",
                    },
                    results: [
                        { changeDate: 1502294526455, value: 1180, dataKey: "Female" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Male" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Unknown" }
                    ]
                },
                {
                    resultType: {
                        i2b2Options: {
                            description: "Vital Status patient breakdown",
                            displayType: "CATNUM"
                        },
                        isBreakdown: true,
                        name: "PATIENT_VITALSTATS_COUNT_XML",
                    },
                    results: [
                        { changeDate: 1502294526455, value: 25, dataKey: "Deceased" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Deferred" },
                        { changeDate: 1502294526455, value: 1155, dataKey: "Living" },
                        { changeDate: 1502294526455, value: -1, dataKey: "Not recorded" }
                    ]
                }
            ]
        }
    ],
    "query": {
        "queryName": "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111",
        "changeDate": 1501001607335,
        "networkId": "2421519216383772161",
        "queryXml": "<runQuery><projectId>SHRINE</projectId><waitTimeMs>180000</waitTimeMs><authenticationInfo><domain>i2b2demo</domain><username>shrine</username><credential isToken=\"true\">SessionKey:HUTBEZkgsz9XyADXjDEO</credential></authenticationInfo><queryId>2421519216383772161</queryId><topicId>3</topicId><topicName>Test dev1 approved</topicName><outputTypes><resultType><id>4</id><name>PATIENT_COUNT_XML</name><isBreakdown>false</isBreakdown><description>Number of patients</description><displayType>CATNUM</displayType></resultType></outputTypes><queryDefinition><name>Female@12:53:25</name><expr><term>\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Female\\</term></expr></queryDefinition></runQuery>",
        "dateCreated": 1501001607334
    }
};