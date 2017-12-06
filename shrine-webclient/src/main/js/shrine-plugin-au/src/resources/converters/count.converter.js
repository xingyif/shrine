export class CountValueConverter {
    toView(value) {
      const PLUS_MINUS_CHAR = '\xB1';
      return value < 0? `10 patients or fewer` : `${value} ${PLUS_MINUS_CHAR} 10 patients`;
    }
}