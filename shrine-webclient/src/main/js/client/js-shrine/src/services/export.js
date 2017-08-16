import {PubSub} from './pub-sub'
const privateProps = new WeakMap();
export class Export extends PubSub{
    constructor(...rest) {
        super(...rest);
        privateProps.set(this, {

        }); 
    }
    listen() {
        this.subscribe(this.commands.shrine.exportResult, convertObjectToCSV);
    }
}

const convertObjectToCSV = (d) => {
    
    const nodeNames = d.nodes.map(n => n.adapterNode);
    const nodes = d.nodes;

    const breakdownMap = new Map();
    nodes.map(({breakdowns, adapterNode})=> 
        breakdowns.map(({resultType:{i2b2Options:{description}}}) => breakdownMap.set(description, {})));
    
    for(const m of breakdownMap) {
        console.log(m);
        
    }
    

    //const list = [];
    //d.nodes.map(n => list.push(...[n.adapterNode],...n.breakdowns));
    //console.log(list);

    


    //read into an array and join it with a '\n'
    let line1 = `data:text/csv;charset=utf-8,SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS),${d.nodes.map(n => n.adapterNode).join(',')}`;
    let line2 = `\nAll Patients,${d.nodes.map(n => n.count).join(',')}`;
    const csv = encodeURI(`${line1}${line2}`);
    const link = document.createElement('a');
    const breakdowns = d.nodes.map(n => n.breakdowns)

    link.setAttribute('href', csv);
    link.setAttribute('download', 'test.csv');
    //link.click();
}