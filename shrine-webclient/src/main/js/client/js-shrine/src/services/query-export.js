import {PubSub} from './pub-sub';
export class QueryExport extends PubSub{
    constructor(...rest) {
        super(...rest); 
    }
    listen() {
        this.subscribe(this.commands.shrine.exportResult, convertObjectToCSV);
    }
}

const convertObjectToCSV = (d) => {
    const nodes = d.nodes.sort();
    const m = new Map();
    const desc = ({resultType:{i2b2Options:{description}}}) => description;
    const brdSort = (a,b) => desc(a) <= desc(b)? -1 : 1;
    nodes.forEach(({breakdowns = []}) => {
        breakdowns.sort(brdSort).forEach(({resultType:{i2b2Options:{description}}, results}) => 
            m.has(description)? m.get(description).add(...results.map(r => r.dataKey).sort()) : m.set(description, new Set(results.map(r => r.dataKey).sort()))
        )
    });

    const line1 = `data:text/csv;charset=utf-8,SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS),${['', ...nodes.map(n => n.adapterNode).join(',')]}`;
    const line2 = `\nAll Patients,${['', ...nodes.map(n => n.count? (n.count > 0? n.count : 0) : 'unavailable').join(',')]}`;
    const result = [];
    m.forEach((v, k) => {
        result.push('',...Array.from(v).map(s => {
            const title = `${k.split(' ').shift()},${s}`;
            const values = nodes.map(({breakdowns = []}) => {
                const b = breakdowns.find(({resultType:{i2b2Options:{description}}, results}) => description === k);
                const r = b? b.results.find(r => r.dataKey === s) : undefined;
                return !r? 'unavailable': r.value > 0? r.value : 0;
            });
            return `${title},${values.join(",")}`;
        }));
    });
    const csv = encodeURI(`${line1}${line2}${result.join('\n')}`);
    const link = document.createElement('a');
    link.setAttribute('href', csv);
    link.setAttribute('download', 'export.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}