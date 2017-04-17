import { BoxStyleValueConverter } from '../../../src/views/query-viewer/box-style.converter';
import { ResultStyleValueConverter } from '../../../src/views/query-viewer/result-style.converter';
import { ResultValueConverter } from '../../../src/views/query-viewer/result-value.converter';

const boxStyleConverter = new BoxStyleValueConverter();
const resultStyleConverter = new ResultStyleValueConverter();
const resultValueConverter = new ResultValueConverter();

describe('Value Converters should convert viewModel values to usable style and text in the view', () => {
    it('Box style converter should transform the box div horizontally based on page index', () => {
        const pageIndex = 2;
        const expectedResult = 'transform: translate(-200%);';
        const result = boxStyleConverter.toView(pageIndex);
        expect(result).toBe(expectedResult);
    });

    it('Result style converter should show red text for any ERROR status', () => {
        const errorStatus = {
            status: "ERROR"
        };
        const expectedResult = 'color:#FF0000';
        const result = resultStyleConverter.toView(errorStatus);
        expect(result).toBe(expectedResult);
    });

    it('Result style converter should show red text for any undefined status', () => {
        const undefinedStatus = undefined;
        const expectedResult = 'color:#FF0000';
        const result = resultStyleConverter.toView(undefinedStatus);
        expect(result).toBe(expectedResult);
    });

    it('Result style converter should show green text for any unrresolved status that is not an error', () => {
        const unresolvedStatus = {
            status: "PENDING"
        };
        const expectedResult = 'color:#00FF00';
        const result = resultStyleConverter.toView(unresolvedStatus);
        expect(result).toBe(expectedResult);
    });

    it('Result value converter should return "not available" for undefinded query status', () => {
        const undefinedStatus = undefined;
        const expectedResult = 'not available';
        const result = resultValueConverter.toView(undefinedStatus);
        expect(result).toBe(expectedResult);

    });

    it('Result value converter should return query result status if it has not completed', () => {
        const unfinishedStatus = {
            status: "ERROR"
        }
        const expectedResult = "ERROR";
        const result = resultValueConverter.toView(unfinishedStatus);
        expect(result).toBe(expectedResult);
    });

    it('Result value converter should reutrn <10 the actual result count is -1.', () => {
        const negativeCount = {
            status: "FINISHED",
            count: -1
        }
        const expectedResult = '<=10';
        const result = resultValueConverter.toView(negativeCount);
        expect(result).toBe(expectedResult);
    });

    it('Result value converter should return the actual count if the query is finished and the count is greater than or equal to 0', () => {
        const nonNegativeCount = {
            status: "FINISHED",
            count: 0
        };
        const expectedResult = 0;
        const result = resultValueConverter.toView(nonNegativeCount);
        expect(result).toBe(expectedResult);
    });

});