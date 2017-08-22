import {PubSub} from './pub-sub';
export class Export extends PubSub{
    constructor(...rest) {
        super(...rest); 
    }
    listen() {
        this.subscribe(this.commands.shrine.exportResult, convertObjectToCSV);
    }
}

const convertObjectToCSV = (d) => {
    const nodeNames = d.nodes.map(n => n.adapterNode);
    const nodes = d.nodes;
    const m = new Map();
    nodes.forEach(({breakdowns}) => 
        breakdowns.forEach(({resultType:{i2b2Options:{description}}, results}) => {
            m.has(description)? m.get(description).add(...results.map(r => r.dataKey)) : m.set(description, new Set(results.map(r => r.dataKey)));
    }));

    const line1 = `data:text/csv;charset=utf-8,SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS),${nodes.map(n => n.adapterNode).join(',')}`;
    const line2 = `\nAll Patients,${nodes.map(n => n.count).join(',')}`;
    const result = [];
    m.forEach((v, k) => {
        result.push('',...Array.from(v).map(s => {
            const title = `${k.split(' ').shift()}|${s}`;
            const values = nodes.map(({breakdowns}) => {
                const b = breakdowns.find(({resultType:{i2b2Options:{description}}, results}) => description === k);
                const r = b? b.results.find(r => {
                    return r.dataKey === s
                }) : undefined;
                return r? r.value : 'unavailable';
            });
            return `${title},${values.join(",")}`;
        }));
    });
    const csv = encodeURI(`${line1}${line2}${result.join('\n')}`);
    const link = document.createElement('a');
    const breakdowns = d.nodes.map(n => n.breakdowns)
    link.setAttribute('href', csv);
    link.setAttribute('download', 'test.csv');
    //link.click();
}