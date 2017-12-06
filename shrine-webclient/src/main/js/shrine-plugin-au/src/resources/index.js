import { PLATFORM } from 'aurelia-pal';
export function configure(aurelia) {
  aurelia.globalResources(
    PLATFORM.moduleName('./converters/count.converter'),
    PLATFORM.moduleName('./converters/datetime.converter'),
    PLATFORM.moduleName('./converters/truncate.converter')
  );

  aurelia.globalResources(
    PLATFORM.moduleName('./custom/breakdown/breakdown'),
    PLATFORM.moduleName('./custom/node-result/node-result'),
    PLATFORM.moduleName('./custom/node-status/node-status'));
}