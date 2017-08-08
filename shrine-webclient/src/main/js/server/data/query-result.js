module.exports = {
 "results": [
  {
   "count": 1185,
   "networkQueryId": 2421519216383772200,
   "statusMessage": "FINISHED",
   "changeDate": 1501001608958,
   "instanceId": 221,
   "resultId": 367,
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
   "adapterNode": "shrine-dev1"
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
   "adapterNode": "shrine-dev2"
  }
 ],
 "query": {
  "queryName": "Female@12:53:25",
  "changeDate": 1501001607335,
  "networkId": "2421519216383772161",
  "queryXml": "<runQuery><projectId>SHRINE</projectId><waitTimeMs>180000</waitTimeMs><authenticationInfo><domain>i2b2demo</domain><username>shrine</username><credential isToken=\"true\">SessionKey:HUTBEZkgsz9XyADXjDEO</credential></authenticationInfo><queryId>2421519216383772161</queryId><topicId>3</topicId><topicName>Test dev1 approved</topicName><outputTypes><resultType><id>4</id><name>PATIENT_COUNT_XML</name><isBreakdown>false</isBreakdown><description>Number of patients</description><displayType>CATNUM</displayType></resultType></outputTypes><queryDefinition><name>Female@12:53:25</name><expr><term>\\\\SHRINE\\SHRINE\\Demographics\\Gender\\Female\\</term></expr></queryDefinition></runQuery>",
  "dateCreated": 1501001607334
 }
};