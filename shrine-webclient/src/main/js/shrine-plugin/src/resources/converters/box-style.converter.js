export class BoxStyleValueConverter {
    toView(value) {
        return 'transform: translate(' + String(-100 * value) + '%);';
    }
}