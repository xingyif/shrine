//see CRC_ctrlr_QryStatus.js line 480ish.

export class QueryStatus {
    constructor() {

        const query = {
            nodeResults: [
                {
                    timestamp: 1490571987946,
                    adapterNode: 'shrine-qa1',
                    statusMessage: 'FINISHED',
                    count: 1810,
                    breakdowns: []
                }, 
                {
                    timestamp: 1490571987946,
                    adapterNode: 'shrine-qa2',
                    statusMessage: 'ERROR',
                    statusDescription: 'Error status 1: Could not map query term(s).',
                    count: 10,
                    breakdowns: []
                },
                {
                    timestamp: 1490571987946,
                    adapterNode: 'shrine-qa3',
                    statusMessage: 'PROCESSING',
                    count: 10,
                    breakdowns: []
                },
                {
                    timestamp: 1490571987946,
                    adapterNode: 'shrine-qa4',
                    statusMessage: 'UNAVAILABLE',
                    count: 10,
                    breakdowns: []
                },
                {
                    timestamp: 1490571987946,
                    adapterNode: 'shrine-qa5',
                    statusMessage: 'FINISHED',
                    count: -1,
                    breakdowns: []
                },
                {
                    timestamp: 1490571987946,
                    adapterNode: 'shrine-qa6',
                    statusMessage: 'ERROR',
                    statusDescription: 'Error status 2: Could not map query term(s).',
                    count: 10,
                    breakdowns: []
                }],
            name: 'Female@12:00:35',
            networkQueryId: 6386518509045377000,
            completed: false
        }

        this.query = query;
    }
    //ERROR,"UNAVAILABLE" - both are error
    //"PROCESSING" - 
    //"COMPLETED", "FINISHED"

}