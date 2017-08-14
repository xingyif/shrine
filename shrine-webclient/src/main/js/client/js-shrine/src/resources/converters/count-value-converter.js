export class CountValueConverter {
    toView(value) {
        return value < 0? `<=10 patients` : `${value} +-10 patients`;
    }
}