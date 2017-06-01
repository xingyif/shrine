module.exports = {
  "adapters": [
    "SHRINE QA Hub",
    "SHRINE QA Node 2",
    "SHRINE QA Node 3",
    "shrine-qa1",
    "shrine-qa2",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3",
    "shrine-qa3"

  ],
  "queryResults": [
    {
      "adaptersToResults": [
        {
          "startDate": 1490984964085,
          "count": -1,
          "networkQueryId": 7768019175359373000,
          "statusMessage": "ERROR",
          "changeDate": 1490984965845,
          "instanceId": 1899,
          "resultId": 2181,
          "status": "ERROR",
          "resultType": {
            "isBreakdown": false,
            "name": "PATIENT_COUNT_XML",
            "id": 4,
            "i2b2Options": {
              "description": "Number of patients",
              "displayType": "CATNUM"
            }
          },
          "endDate": 1490984964917,
          "adapterNode": "shrine-qa3"
        },
        {
          "startDate": 1490984962460,
          "count": -1,
          "networkQueryId": 7768019175359373000,
          "statusMessage": "ERROR",
          "changeDate": 1490984965908,
          "instanceId": 875,
          "resultId": 1072,
          "status": "ERROR",
          "resultType": {
            "isBreakdown": false,
            "name": "PATIENT_COUNT_XML",
            "id": 4,
            "i2b2Options": {
              "description": "Number of patients",
              "displayType": "CATNUM"
            }
          },
          "endDate": 1490984963403,
          "adapterNode": "shrine-qa2"
        },
        {
          "startDate": 1490984962368,
          "count": -1,
          "networkQueryId": 7768019175359373000,
          "statusMessage": "ERROR",
          "changeDate": 1490984965923,
          "instanceId": 1133,
          "resultId": 1330,
          "status": "ERROR",
          "resultType": {
            "isBreakdown": false,
            "name": "PATIENT_COUNT_XML",
            "id": 4,
            "i2b2Options": {
              "description": "Number of patients",
              "displayType": "CATNUM"
            }
          },
          "endDate": 1490984963324,
          "adapterNode": "shrine-qa1"
        }
      ],
      "query": {
        "expression": "Constrained(Term(\\\\SHRINE\\SHRINE\\Diagnoses\\Certain conditions originating in the perinatal period (760-779.99)\\),Some(Modifiers(Admit Diagnosis,\\SHRINE\\Diagnoses\\Certain conditions originating in the perinatal period (760-779.99)\\%,\\\\SHRINE\\Admit Diagnosis\\)),None)",
        "queryName": "Admit Diagnosis@14:29:22",
        "deleted": false,
        "changeDate": 1490984963326,
        "userName": "ij22",
        "networkId": 7768019175359373000,
        "queryXml": "<runQuery><projectId>SHRINE</projectId><waitTimeMs>180000</waitTimeMs><authenticationInfo><domain>i2b2demo</domain><username>ij22</username><credential isToken=\"true\">SessionKey:U9v3LeZcEkbdTCfWLQi7</credential></authenticationInfo><queryId>7768019175359372886</queryId><topicId>3</topicId><topicName>medication only_27th march</topicName><outputTypes><resultType><id>4</id><name>PATIENT_COUNT_XML</name><isBreakdown>false</isBreakdown><description>Number of patients</description><displayType>CATNUM</displayType></resultType></outputTypes><queryDefinition><name>Admit Diagnosis@14:29:22</name><expr><constrainedTerm><term>\\\\SHRINE\\SHRINE\\Diagnoses\\Certain conditions originating in the perinatal period (760-779.99)\\</term><modifier><name>Admit Diagnosis</name><appliedPath>\\SHRINE\\Diagnoses\\Certain conditions originating in the perinatal period (760-779.99)\\%</appliedPath><key>\\\\SHRINE\\Admit Diagnosis\\</key></modifier></constrainedTerm></expr></queryDefinition></runQuery>",
        "dateCreated": 1490984963326,
        "userDomain": "i2b2demo"
      }
    },
    {
      "adaptersToResults": [
        {
          "startDate": 1490571995243,
          "count": 60,
          "networkQueryId": 2226563841174084900,
          "statusMessage": "ERROR",
          "changeDate": 1490572000244,
          "instanceId": 832,
          "resultId": 1018,
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
          "endDate": 1490571996337,
          "adapterNode": "SHRINE QA Node 2"
        },
        {
          "startDate": 1490571995232,
          "count": 50,
          "networkQueryId": 2226563841174084900,
          "statusMessage": "FINISHED",
          "changeDate": 1490572000263,
          "instanceId": 1058,
          "resultId": 1244,
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
          "endDate": 1490571996293,
          "adapterNode": "SHRINE QA Hub"
        },
        {
          "startDate": 1490571997183,
          "count": 60,
          "networkQueryId": 2226563841174084900,
          "statusMessage": "FINISHED",
          "changeDate": 1490572000275,
          "instanceId": 1856,
          "resultId": 2127,
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
          "endDate": 1490571998202,
          "adapterNode": "SHRINE QA Node 3"
        }
      ],
      "query": {
        "expression": "Term(\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Female\\)",
        "queryName": "Female@19:46:33",
        "deleted": false,
        "changeDate": 1490571996396,
        "userName": "ij22",
        "networkId": 2226563841174084900,
        "queryXml": "<runQuery><projectId>SHRINE</projectId><waitTimeMs>180000</waitTimeMs><authenticationInfo><domain>i2b2demo</domain><username>ij22</username><credential isToken=\"true\">SessionKey:Gv6xaK4AQNjQipmypc7p</credential></authenticationInfo><queryId>2226563841174084953</queryId><topicId>3</topicId><topicName>medication only_27th march</topicName><outputTypes><resultType><name>PATIENT_AGE_COUNT_XML</name><isBreakdown>true</isBreakdown><description>Age patient breakdown</description><displayType>CATNUM</displayType></resultType><resultType><id>4</id><name>PATIENT_COUNT_XML</name><isBreakdown>false</isBreakdown><description>Number of patients</description><displayType>CATNUM</displayType></resultType><resultType><name>PATIENT_GENDER_COUNT_XML</name><isBreakdown>true</isBreakdown><description>Gender patient breakdown</description><displayType>CATNUM</displayType></resultType><resultType><name>PATIENT_RACE_COUNT_XML</name><isBreakdown>true</isBreakdown><description>Race patient breakdown</description><displayType>CATNUM</displayType></resultType><resultType><name>PATIENT_VITALSTATUS_COUNT_XML</name><isBreakdown>true</isBreakdown><description>Vital Status patient breakdown</description><displayType>CATNUM</displayType></resultType></outputTypes><queryDefinition><name>Female@19:46:33</name><expr><term>\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Female\\</term></expr></queryDefinition></runQuery>",
        "dateCreated": 1490571996395,
        "userDomain": "i2b2demo"
      }
    },
    {
      "adaptersToResults": [
        {
          "startDate": 1490571983703,
          "count": -1,
          "networkQueryId": 6386518509045377000,
          "statusMessage": "FINISHED",
          "changeDate": 1490571987946,
          "instanceId": 831,
          "resultId": 1016,
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
          "endDate": 1490571985231,
          "adapterNode": "SHRINE QA Node 2"
        },
        {
          "startDate": 1490571983939,
          "count": -1,
          "networkQueryId": 6386518509045377000,
          "statusMessage": "FINISHED",
          "changeDate": 1490571987953,
          "instanceId": 1057,
          "resultId": 1242,
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
          "endDate": 1490571985580,
          "adapterNode": "SHRINE QA Hub"
        },
        {
          "startDate": 1490571985806,
          "count": -1,
          "networkQueryId": 6386518509045377000,
          "statusMessage": "FINISHED",
          "changeDate": 1490571987957,
          "instanceId": 1855,
          "resultId": 2125,
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
          "endDate": 1490571987400,
          "adapterNode": "SHRINE QA Node 3"
        }
      ],
      "query": {
        "expression": "Term(\\\\SHRINE\\SHRINE\\Demographics\\Age\\0-9 years old\\)",
        "queryName": "0-9 years old@19:46:24",
        "deleted": false,
        "changeDate": 1490571985154,
        "userName": "ij22",
        "networkId": 6386518509045377000,
        "queryXml": "<runQuery><projectId>SHRINE</projectId><waitTimeMs>180000</waitTimeMs><authenticationInfo><domain>i2b2demo</domain><username>ij22</username><credential isToken=\"true\">SessionKey:Gv6xaK4AQNjQipmypc7p</credential></authenticationInfo><queryId>6386518509045377410</queryId><topicId>3</topicId><topicName>medication only_27th march</topicName><outputTypes><resultType><id>4</id><name>PATIENT_COUNT_XML</name><isBreakdown>false</isBreakdown><description>Number of patients</description><displayType>CATNUM</displayType></resultType></outputTypes><queryDefinition><name>0-9 years old@19:46:24</name><expr><term>\\\\SHRINE\\SHRINE\\Demographics\\Age\\0-9 years old\\</term></expr></queryDefinition></runQuery>",
        "dateCreated": 1490571985154,
        "userDomain": "i2b2demo"
      }
    },
    {
      "adaptersToResults": [
        {
          "startDate": 1490571976696,
          "count": 40,
          "networkQueryId": 2400531532595122000,
          "statusMessage": "FINISHED",
          "changeDate": 1490571979929,
          "instanceId": 830,
          "resultId": 1015,
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
          "endDate": 1490571977377,
          "adapterNode": "SHRINE QA Node 2"
        },
        {
          "startDate": 1490571976829,
          "count": 55,
          "networkQueryId": 2400531532595122000,
          "statusMessage": "FINISHED",
          "changeDate": 1490571979935,
          "instanceId": 1056,
          "resultId": 1241,
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
          "endDate": 1490571977623,
          "adapterNode": "SHRINE QA Hub"
        },
        {
          "startDate": 1490571978660,
          "count": 40,
          "networkQueryId": 2400531532595122000,
          "statusMessage": "ERROR",
          "changeDate": 1490571979942,
          "instanceId": 1854,
          "resultId": 2124,
          "status": "ERROR",
          "resultType": {
            "isBreakdown": false,
            "name": "PATIENT_COUNT_XML",
            "id": 4,
            "i2b2Options": {
              "description": "Number of patients",
              "displayType": "CATNUM"
            }
          },
          "endDate": 1490571979396,
          "adapterNode": "SHRINE QA Node 3"
        }
      ],
      "query": {
        "expression": "Term(\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Female\\)",
        "queryName": "Female@19:46:17",
        "deleted": false,
        "changeDate": 1490571978065,
        "userName": "ij22",
        "networkId": 2400531532595122000,
        "queryXml": "<runQuery><projectId>SHRINE</projectId><waitTimeMs>180000</waitTimeMs><authenticationInfo><domain>i2b2demo</domain><username>ij22</username><credential isToken=\"true\">SessionKey:Gv6xaK4AQNjQipmypc7p</credential></authenticationInfo><queryId>2400531532595122336</queryId><topicId>3</topicId><topicName>medication only_27th march</topicName><outputTypes><resultType><id>4</id><name>PATIENT_COUNT_XML</name><isBreakdown>false</isBreakdown><description>Number of patients</description><displayType>CATNUM</displayType></resultType></outputTypes><queryDefinition><name>Female@19:46:17</name><expr><term>\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Female\\</term></expr></queryDefinition></runQuery>",
        "dateCreated": 1490571978064,
        "userDomain": "i2b2demo"
      }
    },
    {
      "adaptersToResults": [
        {
          "startDate": 1490571965504,
          "count": -1,
          "networkQueryId": 1813286953364395500,
          "statusMessage": "ERROR",
          "changeDate": 1490571967830,
          "instanceId": 1853,
          "resultId": 2123,
          "status": "ERROR",
          "resultType": {
            "isBreakdown": false,
            "name": "PATIENT_COUNT_XML",
            "id": 4,
            "i2b2Options": {
              "description": "Number of patients",
              "displayType": "CATNUM"
            }
          },
          "endDate": 1490571966969,
          "adapterNode": "SHRINE QA Node 3"
        },
        {
          "startDate": 1490571963634,
          "count": -1,
          "networkQueryId": 1813286953364395500,
          "statusMessage": "ERROR",
          "changeDate": 1490571967857,
          "instanceId": 829,
          "resultId": 1014,
          "status": "ERROR",
          "resultType": {
            "isBreakdown": false,
            "name": "PATIENT_COUNT_XML",
            "id": 4,
            "i2b2Options": {
              "description": "Number of patients",
              "displayType": "CATNUM"
            }
          },
          "endDate": 1490571965423,
          "adapterNode": "SHRINE QA Node 2"
        },
        {
          "startDate": 1490571963687,
          "count": -1,
          "networkQueryId": 1813286953364395500,
          "statusMessage": "ERROR",
          "changeDate": 1490571967872,
          "instanceId": 1055,
          "resultId": 1240,
          "status": "ERROR",
          "resultType": {
            "isBreakdown": false,
            "name": "PATIENT_COUNT_XML",
            "id": 4,
            "i2b2Options": {
              "description": "Number of patients",
              "displayType": "CATNUM"
            }
          },
          "endDate": 1490571965339,
          "adapterNode": "SHRINE QA Hub"
        }
      ],
      "query": {
        "expression": "Term(\\\\SHRINE\\SHRINE\\Demographics\\Age\\0-9 years old\\)",
        "queryName": "0-9 years old@19:46:03",
        "deleted": false,
        "changeDate": 1490571964673,
        "userName": "ij22",
        "networkId": 1813286953364395500,
        "queryXml": "<runQuery><projectId>SHRINE</projectId><waitTimeMs>180000</waitTimeMs><authenticationInfo><domain>i2b2demo</domain><username>ij22</username><credential isToken=\"true\">SessionKey:Gv6xaK4AQNjQipmypc7p</credential></authenticationInfo><queryId>1813286953364395437</queryId><topicId>2</topicId><topicName>26th march 2017</topicName><outputTypes><resultType><id>4</id><name>PATIENT_COUNT_XML</name><isBreakdown>false</isBreakdown><description>Number of patients</description><displayType>CATNUM</displayType></resultType></outputTypes><queryDefinition><name>0-9 years old@19:46:03</name><expr><term>\\\\SHRINE\\SHRINE\\Demographics\\Age\\0-9 years old\\</term></expr></queryDefinition></runQuery>",
        "dateCreated": 1490571964672,
        "userDomain": "i2b2demo"
      }
    }
  ],
  "flags": {}
}