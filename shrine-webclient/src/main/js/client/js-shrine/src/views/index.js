export function configure(aurelia) {

    const views = [
        'views/query-status/query-status'
    ];
    aurelia.globalResources(...views);
}