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

    const line1 = `SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS),${['', ...nodes.map(n => n.adapterNode).join(',')]}`;
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
    const csv = `${line1}${line2}${result.join('\n')}`;
    window.navigator && window.navigator.msSaveOrOpenBlob? exportInIE(csv) : exportInWebkitGecko(csv);
}
const exportInIE = csv => {
    const blob = new Blob([decodeURIComponent(encodeURI(csv))], {
        type: 'text/csv;charset=utf-8;'
      });
    window.navigator.msSaveOrOpenBlob(blob, 'export.csv');
    return csv;
}

const exportInWebkitGecko = csv => {
    const link = document.createElement('a');
    const evt = document.createEvent('MouseEvents');
    !link.download? link.target = '_blank' :  link.download = 'export.csv'
    link.href = 'data:application/csv;charset=utf-8,' + encodeURIComponent(csv);
    document.body.appendChild(link);
    evt.initEvent( 'click', true, true );
    link.dispatchEvent(evt);
}