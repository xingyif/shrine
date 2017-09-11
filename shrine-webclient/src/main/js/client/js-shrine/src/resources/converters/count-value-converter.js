export class CountValueConverter {
    toView(value) {
        return value < 0? `10 patients or fewer` : `${value} +-10 patients`;
    }
}