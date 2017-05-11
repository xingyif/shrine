export class Container {
    constructor(v) {
        this.__value = v;
    }

    static of(value) {
        return new Container(value);
    }

    get value() {
        return this.__value;
    }

    map(f) {
        return this.hasNothing() ? Container.of(null) : Container.of(f(this.value));
    }

    hasNothing() {
        return this.value === null || this.value === undefined;
    }
}