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
    exportIEWebkitGecko(csv);
}

const exportIEWebkitGecko = csv => {
    const filename = "export.csv";
    const blob = new Blob([csv]);
    const isIE = window.navigator !== undefined && window.navigator.msSaveOrOpenBlob !== undefined
    if (isIE) window.navigator.msSaveBlob(blob, filename);
    else {
        const a = window.document.createElement("a");
        a.href = window.URL.createObjectURL(blob, {type: "text/plain"});
        a.download = filename;
        document.body.appendChild(a);
        a.click();  // IE: "Access is denied"; see: https://connect.microsoft.com/IE/feedback/details/797361/ie-10-treats-blob-url-as-cross-origin-and-denies-access
        document.body.removeChild(a);
    }
}