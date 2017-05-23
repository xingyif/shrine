import * as _ from 'ramda';
import {Container} from 'common/container';
export class ScrollService{
    static either = _.curry((el, d, c) => Container.of(_.prop(el, c) || d));
    
    // -- todo: join() method -- //
    static target = (p, c) => ScrollService.either('target', c, c)
        .chain((v) => ScrollService.either(p, 0, v));

    static clientHeight = e => ScrollService.target('clientHeight', e);
    static scrollHeight = e => ScrollService.target('scrollHeight', e);
    static scrollTop = e => ScrollService.target('scrollTop', e);

    static userScroll = e => ScrollService.clientHeight(e)
        .map(v => v + ScrollService.scrollTop(e).value);

    static scrollRatio = e => ScrollService.userScroll(e)
        .map(v => v / ScrollService.scrollHeight(e).value);
}