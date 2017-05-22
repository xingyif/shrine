System.register(['ramda', 'common/container'], function (_export, _context) {
    "use strict";

    var _, Container, _class, _temp, ScrollService;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_ramda) {
            _ = _ramda;
        }, function (_commonContainer) {
            Container = _commonContainer.Container;
        }],
        execute: function () {
            _export('ScrollService', ScrollService = (_temp = _class = function ScrollService() {
                _classCallCheck(this, ScrollService);
            }, _class.either = _.curry(function (el, d, c) {
                return Container.of(_.prop(el, c) || d);
            }), _class.target = function (p, c) {
                return ScrollService.either('target', c, c).map(function (v) {
                    return ScrollService.either(p, 0, v).value;
                });
            }, _class.clientHeight = function (e) {
                return ScrollService.target('clientHeight', e);
            }, _class.scrollHeight = function (e) {
                return ScrollService.target('scrollHeight', e);
            }, _class.scrollTop = function (e) {
                return ScrollService.target('scrollTop', e);
            }, _class.userScroll = function (e) {
                return ScrollService.clientHeight(e).map(function (v) {
                    return v + ScrollService.scrollTop(e).value;
                });
            }, _class.scrollRatio = function (e) {
                return ScrollService.userScroll(e).map(function (v) {
                    return v / ScrollService.scrollHeight(e).value;
                });
            }, _temp));

            _export('ScrollService', ScrollService);
        }
    };
});
//# sourceMappingURL=scroll.service.js.map
