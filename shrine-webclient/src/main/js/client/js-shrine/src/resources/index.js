export function configure(aurelia) {

    const converterPrefix = 'converters';
    const converters = [
        'box-style.converter',
        'count-value-converter',
        'datetime.value.converter',
        'result-style.converter',
        'result-value.converter'
    ];
    aurelia.globalResources(...converters.map(c => `./${converterPrefix}/${c}`));

    const customPrefix = 'custom';
    const custom = [
        'error/error',
        'breakdown/breakdown',
        'node-result/node-result',
        'node-status/node-status',
        'patient-count/patient-count'
    ];
    aurelia.globalResources(...custom.map(c => `./${customPrefix}/${c}`));
}