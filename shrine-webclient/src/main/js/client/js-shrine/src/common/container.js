import * as _ from 'ramda';
export class Container {
    constructor(f) {
        this.__value = f;
    }

    static of(value) {
        return new Container(function() {
            return value;
        });
    }

    get value() {
        return this.__value();
    }

    map(f) {
        return this.hasNothing() ? Container.of(null) : Container.of(f(this.value));
    }

    hasNothing() {
        return this.value === null || this.value === undefined;
    }
}