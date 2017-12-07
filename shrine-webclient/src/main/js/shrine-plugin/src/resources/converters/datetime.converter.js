import moment from 'moment';
export class DateTimeValueConverter {
  toView(value) {
    return moment(value).format('MM/DD/YYYY h:mm:ss a');
  }
}