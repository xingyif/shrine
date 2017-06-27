System.config({
  defaultJSExtensions: true,
  transpiler: false,
  paths: {
    "*": "dist/*",
    "github:*": "jspm_packages/github/*",
    "npm:*": "jspm_packages/npm/*"
  },
  map: {
    "aurelia-animator-css": "npm:aurelia-animator-css@1.0.1",
    "aurelia-bootstrapper": "npm:aurelia-bootstrapper@1.0.0",
    "aurelia-event-aggregator": "npm:aurelia-event-aggregator@1.0.1",
    "aurelia-fetch-client": "npm:aurelia-fetch-client@1.0.1",
    "aurelia-framework": "npm:aurelia-framework@1.0.6",
    "aurelia-history-browser": "npm:aurelia-history-browser@1.0.0",
    "aurelia-loader-default": "npm:aurelia-loader-default@1.0.0",
    "aurelia-logging-console": "npm:aurelia-logging-console@1.0.0",
    "aurelia-pal-browser": "npm:aurelia-pal-browser@1.0.0",
    "aurelia-polyfills": "npm:aurelia-polyfills@1.1.1",
    "aurelia-router": "npm:aurelia-router@1.3.0",
    "aurelia-templating-binding": "npm:aurelia-templating-binding@1.0.0",
    "aurelia-templating-resources": "npm:aurelia-templating-resources@1.1.1",
    "aurelia-templating-router": "npm:aurelia-templating-router@1.0.0",
    "bluebird": "npm:bluebird@3.4.1",
    "bootstrap": "github:twbs/bootstrap@3.3.7",
    "fetch": "github:github/fetch@1.0.0",
    "font-awesome": "npm:font-awesome@4.7.0",
    "jquery": "npm:jquery@2.2.4",
    "ramda": "npm:ramda@0.23.0",
    "text": "github:systemjs/plugin-text@0.0.8",
    "github:jspm/nodelibs-assert@0.1.0": {
      "assert": "npm:assert@1.4.1"
    },
    "github:jspm/nodelibs-buffer@0.1.1": {
      "buffer": "npm:buffer@5.0.6"
    },
    "github:jspm/nodelibs-process@0.1.2": {
      "process": "npm:process@0.11.10"
    },
    "github:jspm/nodelibs-util@0.1.0": {
      "util": "npm:util@0.10.3"
    },
    "github:jspm/nodelibs-vm@0.1.0": {
      "vm-browserify": "npm:vm-browserify@0.0.4"
    },
    "github:twbs/bootstrap@3.3.7": {
      "jquery": "npm:jquery@2.2.4"
    },
    "npm:assert@1.4.1": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "buffer": "github:jspm/nodelibs-buffer@0.1.1",
      "process": "github:jspm/nodelibs-process@0.1.2",
      "util": "npm:util@0.10.3"
    },
    "npm:aurelia-animator-css@1.0.1": {
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-pal": "npm:aurelia-pal@1.3.0",
      "aurelia-templating": "npm:aurelia-templating@1.1.1"
    },
    "npm:aurelia-binding@1.0.9": {
      "aurelia-logging": "npm:aurelia-logging@1.3.1",
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-pal": "npm:aurelia-pal@1.3.0",
      "aurelia-task-queue": "npm:aurelia-task-queue@1.1.0"
    },
    "npm:aurelia-bootstrapper@1.0.0": {
      "aurelia-event-aggregator": "npm:aurelia-event-aggregator@1.0.1",
      "aurelia-framework": "npm:aurelia-framework@1.0.6",
      "aurelia-history": "npm:aurelia-history@1.0.0",
      "aurelia-history-browser": "npm:aurelia-history-browser@1.0.0",
      "aurelia-loader-default": "npm:aurelia-loader-default@1.0.0",
      "aurelia-logging-console": "npm:aurelia-logging-console@1.0.0",
      "aurelia-pal": "npm:aurelia-pal@1.3.0",
      "aurelia-pal-browser": "npm:aurelia-pal-browser@1.0.0",
      "aurelia-polyfills": "npm:aurelia-polyfills@1.1.1",
      "aurelia-router": "npm:aurelia-router@1.3.0",
      "aurelia-templating": "npm:aurelia-templating@1.1.1",
      "aurelia-templating-binding": "npm:aurelia-templating-binding@1.0.0",
      "aurelia-templating-resources": "npm:aurelia-templating-resources@1.1.1",
      "aurelia-templating-router": "npm:aurelia-templating-router@1.0.0"
    },
    "npm:aurelia-dependency-injection@1.3.1": {
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-pal": "npm:aurelia-pal@1.3.0"
    },
    "npm:aurelia-event-aggregator@1.0.1": {
      "aurelia-logging": "npm:aurelia-logging@1.3.1"
    },
    "npm:aurelia-framework@1.0.6": {
      "aurelia-binding": "npm:aurelia-binding@1.0.9",
      "aurelia-dependency-injection": "npm:aurelia-dependency-injection@1.3.1",
      "aurelia-loader": "npm:aurelia-loader@1.0.0",
      "aurelia-logging": "npm:aurelia-logging@1.3.1",
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-pal": "npm:aurelia-pal@1.3.0",
      "aurelia-path": "npm:aurelia-path@1.1.1",
      "aurelia-task-queue": "npm:aurelia-task-queue@1.1.0",
      "aurelia-templating": "npm:aurelia-templating@1.1.1"
    },
    "npm:aurelia-history-browser@1.0.0": {
      "aurelia-history": "npm:aurelia-history@1.0.0",
      "aurelia-pal": "npm:aurelia-pal@1.3.0"
    },
    "npm:aurelia-loader-default@1.0.0": {
      "aurelia-loader": "npm:aurelia-loader@1.0.0",
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-pal": "npm:aurelia-pal@1.3.0"
    },
    "npm:aurelia-loader@1.0.0": {
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-path": "npm:aurelia-path@1.1.1"
    },
    "npm:aurelia-logging-console@1.0.0": {
      "aurelia-logging": "npm:aurelia-logging@1.3.1"
    },
    "npm:aurelia-metadata@1.0.3": {
      "aurelia-pal": "npm:aurelia-pal@1.3.0"
    },
    "npm:aurelia-pal-browser@1.0.0": {
      "aurelia-pal": "npm:aurelia-pal@1.3.0"
    },
    "npm:aurelia-polyfills@1.1.1": {
      "aurelia-pal": "npm:aurelia-pal@1.3.0"
    },
    "npm:aurelia-route-recognizer@1.1.0": {
      "aurelia-path": "npm:aurelia-path@1.1.1"
    },
    "npm:aurelia-router@1.3.0": {
      "aurelia-dependency-injection": "npm:aurelia-dependency-injection@1.3.1",
      "aurelia-event-aggregator": "npm:aurelia-event-aggregator@1.0.1",
      "aurelia-history": "npm:aurelia-history@1.0.0",
      "aurelia-logging": "npm:aurelia-logging@1.3.1",
      "aurelia-path": "npm:aurelia-path@1.1.1",
      "aurelia-route-recognizer": "npm:aurelia-route-recognizer@1.1.0"
    },
    "npm:aurelia-task-queue@1.1.0": {
      "aurelia-pal": "npm:aurelia-pal@1.3.0"
    },
    "npm:aurelia-templating-binding@1.0.0": {
      "aurelia-binding": "npm:aurelia-binding@1.0.9",
      "aurelia-logging": "npm:aurelia-logging@1.3.1",
      "aurelia-templating": "npm:aurelia-templating@1.1.1"
    },
    "npm:aurelia-templating-resources@1.1.1": {
      "aurelia-binding": "npm:aurelia-binding@1.0.9",
      "aurelia-dependency-injection": "npm:aurelia-dependency-injection@1.3.1",
      "aurelia-loader": "npm:aurelia-loader@1.0.0",
      "aurelia-logging": "npm:aurelia-logging@1.3.1",
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-pal": "npm:aurelia-pal@1.3.0",
      "aurelia-path": "npm:aurelia-path@1.1.1",
      "aurelia-task-queue": "npm:aurelia-task-queue@1.1.0",
      "aurelia-templating": "npm:aurelia-templating@1.1.1"
    },
    "npm:aurelia-templating-router@1.0.0": {
      "aurelia-dependency-injection": "npm:aurelia-dependency-injection@1.3.1",
      "aurelia-logging": "npm:aurelia-logging@1.3.1",
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-pal": "npm:aurelia-pal@1.3.0",
      "aurelia-path": "npm:aurelia-path@1.1.1",
      "aurelia-router": "npm:aurelia-router@1.3.0",
      "aurelia-templating": "npm:aurelia-templating@1.1.1"
    },
    "npm:aurelia-templating@1.1.1": {
      "aurelia-binding": "npm:aurelia-binding@1.0.9",
      "aurelia-dependency-injection": "npm:aurelia-dependency-injection@1.3.1",
      "aurelia-loader": "npm:aurelia-loader@1.0.0",
      "aurelia-logging": "npm:aurelia-logging@1.3.1",
      "aurelia-metadata": "npm:aurelia-metadata@1.0.3",
      "aurelia-pal": "npm:aurelia-pal@1.3.0",
      "aurelia-path": "npm:aurelia-path@1.1.1",
      "aurelia-task-queue": "npm:aurelia-task-queue@1.1.0"
    },
    "npm:bluebird@3.4.1": {
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:buffer@5.0.6": {
      "base64-js": "npm:base64-js@1.2.0",
      "ieee754": "npm:ieee754@1.1.8"
    },
    "npm:font-awesome@4.7.0": {
      "css": "github:systemjs/plugin-css@0.1.33"
    },
    "npm:inherits@2.0.1": {
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:process@0.11.10": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "vm": "github:jspm/nodelibs-vm@0.1.0"
    },
    "npm:ramda@0.23.0": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.2",
      "util": "github:jspm/nodelibs-util@0.1.0",
      "vm": "github:jspm/nodelibs-vm@0.1.0"
    },
    "npm:util@0.10.3": {
      "inherits": "npm:inherits@2.0.1",
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:vm-browserify@0.0.4": {
      "indexof": "npm:indexof@0.0.1"
    }
  },
  bundles: {
    "app-build.js": [
      "common/container.js",
      "common/i2b2.pub-sub.js",
      "common/i2b2.service.js",
      "common/queries.model.js",
      "common/shrine.messages.js",
      "common/tabs.model.js",
      "main.js",
      "repository/qep.repository.js",
      "shell.html!github:systemjs/plugin-text@0.0.8.js",
      "shell.js",
      "views/mailto/mailto.config.js",
      "views/mailto/mailto.html!github:systemjs/plugin-text@0.0.8.js",
      "views/mailto/mailto.js",
      "views/mailto/mailto.service.js",
      "views/query-viewer/box-style.converter.js",
      "views/query-viewer/context-menu/context-menu.html!github:systemjs/plugin-text@0.0.8.js",
      "views/query-viewer/context-menu/context-menu.js",
      "views/query-viewer/loading-bar/loading-bar.html!github:systemjs/plugin-text@0.0.8.js",
      "views/query-viewer/loading-bar/loading-bar.js",
      "views/query-viewer/loading-bar/row-loader.html!github:systemjs/plugin-text@0.0.8.js",
      "views/query-viewer/query-viewer.config.js",
      "views/query-viewer/query-viewer.html!github:systemjs/plugin-text@0.0.8.js",
      "views/query-viewer/query-viewer.js",
      "views/query-viewer/query-viewer.model.js",
      "views/query-viewer/query-viewer.service.js",
      "views/query-viewer/result-style.converter.js",
      "views/query-viewer/result-value.converter.js",
      "views/query-viewer/scroll.service.js"
    ],
    "aurelia.js": [
      "github:github/fetch@1.0.0.js",
      "github:github/fetch@1.0.0/fetch.js",
      "github:jspm/nodelibs-process@0.1.2.js",
      "github:jspm/nodelibs-process@0.1.2/index.js",
      "npm:aurelia-binding@1.0.9.js",
      "npm:aurelia-binding@1.0.9/aurelia-binding.js",
      "npm:aurelia-bootstrapper@1.0.0.js",
      "npm:aurelia-bootstrapper@1.0.0/aurelia-bootstrapper.js",
      "npm:aurelia-dependency-injection@1.3.1.js",
      "npm:aurelia-dependency-injection@1.3.1/aurelia-dependency-injection.js",
      "npm:aurelia-event-aggregator@1.0.1.js",
      "npm:aurelia-event-aggregator@1.0.1/aurelia-event-aggregator.js",
      "npm:aurelia-fetch-client@1.0.1.js",
      "npm:aurelia-fetch-client@1.0.1/aurelia-fetch-client.js",
      "npm:aurelia-framework@1.0.6.js",
      "npm:aurelia-framework@1.0.6/aurelia-framework.js",
      "npm:aurelia-history-browser@1.0.0.js",
      "npm:aurelia-history-browser@1.0.0/aurelia-history-browser.js",
      "npm:aurelia-history@1.0.0.js",
      "npm:aurelia-history@1.0.0/aurelia-history.js",
      "npm:aurelia-loader-default@1.0.0.js",
      "npm:aurelia-loader-default@1.0.0/aurelia-loader-default.js",
      "npm:aurelia-loader@1.0.0.js",
      "npm:aurelia-loader@1.0.0/aurelia-loader.js",
      "npm:aurelia-logging-console@1.0.0.js",
      "npm:aurelia-logging-console@1.0.0/aurelia-logging-console.js",
      "npm:aurelia-logging@1.3.1.js",
      "npm:aurelia-logging@1.3.1/aurelia-logging.js",
      "npm:aurelia-metadata@1.0.3.js",
      "npm:aurelia-metadata@1.0.3/aurelia-metadata.js",
      "npm:aurelia-pal-browser@1.0.0.js",
      "npm:aurelia-pal-browser@1.0.0/aurelia-pal-browser.js",
      "npm:aurelia-pal@1.3.0.js",
      "npm:aurelia-pal@1.3.0/aurelia-pal.js",
      "npm:aurelia-path@1.1.1.js",
      "npm:aurelia-path@1.1.1/aurelia-path.js",
      "npm:aurelia-polyfills@1.1.1.js",
      "npm:aurelia-polyfills@1.1.1/aurelia-polyfills.js",
      "npm:aurelia-route-recognizer@1.1.0.js",
      "npm:aurelia-route-recognizer@1.1.0/aurelia-route-recognizer.js",
      "npm:aurelia-router@1.3.0.js",
      "npm:aurelia-router@1.3.0/aurelia-router.js",
      "npm:aurelia-task-queue@1.1.0.js",
      "npm:aurelia-task-queue@1.1.0/aurelia-task-queue.js",
      "npm:aurelia-templating-binding@1.0.0.js",
      "npm:aurelia-templating-binding@1.0.0/aurelia-templating-binding.js",
      "npm:aurelia-templating-resources@1.1.1.js",
      "npm:aurelia-templating-resources@1.1.1/abstract-repeater.js",
      "npm:aurelia-templating-resources@1.1.1/analyze-view-factory.js",
      "npm:aurelia-templating-resources@1.1.1/array-repeat-strategy.js",
      "npm:aurelia-templating-resources@1.1.1/attr-binding-behavior.js",
      "npm:aurelia-templating-resources@1.1.1/aurelia-hide-style.js",
      "npm:aurelia-templating-resources@1.1.1/aurelia-templating-resources.js",
      "npm:aurelia-templating-resources@1.1.1/binding-mode-behaviors.js",
      "npm:aurelia-templating-resources@1.1.1/binding-signaler.js",
      "npm:aurelia-templating-resources@1.1.1/compose.js",
      "npm:aurelia-templating-resources@1.1.1/css-resource.js",
      "npm:aurelia-templating-resources@1.1.1/debounce-binding-behavior.js",
      "npm:aurelia-templating-resources@1.1.1/dynamic-element.js",
      "npm:aurelia-templating-resources@1.1.1/focus.js",
      "npm:aurelia-templating-resources@1.1.1/hide.js",
      "npm:aurelia-templating-resources@1.1.1/html-resource-plugin.js",
      "npm:aurelia-templating-resources@1.1.1/html-sanitizer.js",
      "npm:aurelia-templating-resources@1.1.1/if.js",
      "npm:aurelia-templating-resources@1.1.1/map-repeat-strategy.js",
      "npm:aurelia-templating-resources@1.1.1/null-repeat-strategy.js",
      "npm:aurelia-templating-resources@1.1.1/number-repeat-strategy.js",
      "npm:aurelia-templating-resources@1.1.1/repeat-strategy-locator.js",
      "npm:aurelia-templating-resources@1.1.1/repeat-utilities.js",
      "npm:aurelia-templating-resources@1.1.1/repeat.js",
      "npm:aurelia-templating-resources@1.1.1/replaceable.js",
      "npm:aurelia-templating-resources@1.1.1/sanitize-html.js",
      "npm:aurelia-templating-resources@1.1.1/set-repeat-strategy.js",
      "npm:aurelia-templating-resources@1.1.1/show.js",
      "npm:aurelia-templating-resources@1.1.1/signal-binding-behavior.js",
      "npm:aurelia-templating-resources@1.1.1/throttle-binding-behavior.js",
      "npm:aurelia-templating-resources@1.1.1/update-trigger-binding-behavior.js",
      "npm:aurelia-templating-resources@1.1.1/with.js",
      "npm:aurelia-templating-router@1.0.0.js",
      "npm:aurelia-templating-router@1.0.0/aurelia-templating-router.js",
      "npm:aurelia-templating-router@1.0.0/route-href.js",
      "npm:aurelia-templating-router@1.0.0/route-loader.js",
      "npm:aurelia-templating-router@1.0.0/router-view.js",
      "npm:aurelia-templating@1.1.1.js",
      "npm:aurelia-templating@1.1.1/aurelia-templating.js",
      "npm:font-awesome@4.7.0.js",
      "npm:font-awesome@4.7.0/css/font-awesome.css!github:systemjs/plugin-css@0.1.33.js",
      "npm:jquery@2.2.4.js",
      "npm:jquery@2.2.4/dist/jquery.js",
      "npm:process@0.11.10.js",
      "npm:process@0.11.10/browser.js",
      "npm:ramda@0.23.0.js",
      "npm:ramda@0.23.0/index.js",
      "npm:ramda@0.23.0/src/F.js",
      "npm:ramda@0.23.0/src/T.js",
      "npm:ramda@0.23.0/src/__.js",
      "npm:ramda@0.23.0/src/add.js",
      "npm:ramda@0.23.0/src/addIndex.js",
      "npm:ramda@0.23.0/src/adjust.js",
      "npm:ramda@0.23.0/src/all.js",
      "npm:ramda@0.23.0/src/allPass.js",
      "npm:ramda@0.23.0/src/always.js",
      "npm:ramda@0.23.0/src/and.js",
      "npm:ramda@0.23.0/src/any.js",
      "npm:ramda@0.23.0/src/anyPass.js",
      "npm:ramda@0.23.0/src/ap.js",
      "npm:ramda@0.23.0/src/aperture.js",
      "npm:ramda@0.23.0/src/append.js",
      "npm:ramda@0.23.0/src/apply.js",
      "npm:ramda@0.23.0/src/applySpec.js",
      "npm:ramda@0.23.0/src/ascend.js",
      "npm:ramda@0.23.0/src/assoc.js",
      "npm:ramda@0.23.0/src/assocPath.js",
      "npm:ramda@0.23.0/src/binary.js",
      "npm:ramda@0.23.0/src/bind.js",
      "npm:ramda@0.23.0/src/both.js",
      "npm:ramda@0.23.0/src/call.js",
      "npm:ramda@0.23.0/src/chain.js",
      "npm:ramda@0.23.0/src/clamp.js",
      "npm:ramda@0.23.0/src/clone.js",
      "npm:ramda@0.23.0/src/comparator.js",
      "npm:ramda@0.23.0/src/complement.js",
      "npm:ramda@0.23.0/src/compose.js",
      "npm:ramda@0.23.0/src/composeK.js",
      "npm:ramda@0.23.0/src/composeP.js",
      "npm:ramda@0.23.0/src/concat.js",
      "npm:ramda@0.23.0/src/cond.js",
      "npm:ramda@0.23.0/src/construct.js",
      "npm:ramda@0.23.0/src/constructN.js",
      "npm:ramda@0.23.0/src/contains.js",
      "npm:ramda@0.23.0/src/converge.js",
      "npm:ramda@0.23.0/src/countBy.js",
      "npm:ramda@0.23.0/src/curry.js",
      "npm:ramda@0.23.0/src/curryN.js",
      "npm:ramda@0.23.0/src/dec.js",
      "npm:ramda@0.23.0/src/defaultTo.js",
      "npm:ramda@0.23.0/src/descend.js",
      "npm:ramda@0.23.0/src/difference.js",
      "npm:ramda@0.23.0/src/differenceWith.js",
      "npm:ramda@0.23.0/src/dissoc.js",
      "npm:ramda@0.23.0/src/dissocPath.js",
      "npm:ramda@0.23.0/src/divide.js",
      "npm:ramda@0.23.0/src/drop.js",
      "npm:ramda@0.23.0/src/dropLast.js",
      "npm:ramda@0.23.0/src/dropLastWhile.js",
      "npm:ramda@0.23.0/src/dropRepeats.js",
      "npm:ramda@0.23.0/src/dropRepeatsWith.js",
      "npm:ramda@0.23.0/src/dropWhile.js",
      "npm:ramda@0.23.0/src/either.js",
      "npm:ramda@0.23.0/src/empty.js",
      "npm:ramda@0.23.0/src/eqBy.js",
      "npm:ramda@0.23.0/src/eqProps.js",
      "npm:ramda@0.23.0/src/equals.js",
      "npm:ramda@0.23.0/src/evolve.js",
      "npm:ramda@0.23.0/src/filter.js",
      "npm:ramda@0.23.0/src/find.js",
      "npm:ramda@0.23.0/src/findIndex.js",
      "npm:ramda@0.23.0/src/findLast.js",
      "npm:ramda@0.23.0/src/findLastIndex.js",
      "npm:ramda@0.23.0/src/flatten.js",
      "npm:ramda@0.23.0/src/flip.js",
      "npm:ramda@0.23.0/src/forEach.js",
      "npm:ramda@0.23.0/src/forEachObjIndexed.js",
      "npm:ramda@0.23.0/src/fromPairs.js",
      "npm:ramda@0.23.0/src/groupBy.js",
      "npm:ramda@0.23.0/src/groupWith.js",
      "npm:ramda@0.23.0/src/gt.js",
      "npm:ramda@0.23.0/src/gte.js",
      "npm:ramda@0.23.0/src/has.js",
      "npm:ramda@0.23.0/src/hasIn.js",
      "npm:ramda@0.23.0/src/head.js",
      "npm:ramda@0.23.0/src/identical.js",
      "npm:ramda@0.23.0/src/identity.js",
      "npm:ramda@0.23.0/src/ifElse.js",
      "npm:ramda@0.23.0/src/inc.js",
      "npm:ramda@0.23.0/src/indexBy.js",
      "npm:ramda@0.23.0/src/indexOf.js",
      "npm:ramda@0.23.0/src/init.js",
      "npm:ramda@0.23.0/src/insert.js",
      "npm:ramda@0.23.0/src/insertAll.js",
      "npm:ramda@0.23.0/src/internal/_Set.js",
      "npm:ramda@0.23.0/src/internal/_aperture.js",
      "npm:ramda@0.23.0/src/internal/_arity.js",
      "npm:ramda@0.23.0/src/internal/_arrayFromIterator.js",
      "npm:ramda@0.23.0/src/internal/_assign.js",
      "npm:ramda@0.23.0/src/internal/_checkForMethod.js",
      "npm:ramda@0.23.0/src/internal/_clone.js",
      "npm:ramda@0.23.0/src/internal/_cloneRegExp.js",
      "npm:ramda@0.23.0/src/internal/_complement.js",
      "npm:ramda@0.23.0/src/internal/_concat.js",
      "npm:ramda@0.23.0/src/internal/_contains.js",
      "npm:ramda@0.23.0/src/internal/_containsWith.js",
      "npm:ramda@0.23.0/src/internal/_createPartialApplicator.js",
      "npm:ramda@0.23.0/src/internal/_curry1.js",
      "npm:ramda@0.23.0/src/internal/_curry2.js",
      "npm:ramda@0.23.0/src/internal/_curry3.js",
      "npm:ramda@0.23.0/src/internal/_curryN.js",
      "npm:ramda@0.23.0/src/internal/_dispatchable.js",
      "npm:ramda@0.23.0/src/internal/_dropLast.js",
      "npm:ramda@0.23.0/src/internal/_dropLastWhile.js",
      "npm:ramda@0.23.0/src/internal/_equals.js",
      "npm:ramda@0.23.0/src/internal/_filter.js",
      "npm:ramda@0.23.0/src/internal/_flatCat.js",
      "npm:ramda@0.23.0/src/internal/_forceReduced.js",
      "npm:ramda@0.23.0/src/internal/_functionName.js",
      "npm:ramda@0.23.0/src/internal/_has.js",
      "npm:ramda@0.23.0/src/internal/_identity.js",
      "npm:ramda@0.23.0/src/internal/_indexOf.js",
      "npm:ramda@0.23.0/src/internal/_isArguments.js",
      "npm:ramda@0.23.0/src/internal/_isArray.js",
      "npm:ramda@0.23.0/src/internal/_isFunction.js",
      "npm:ramda@0.23.0/src/internal/_isInteger.js",
      "npm:ramda@0.23.0/src/internal/_isNumber.js",
      "npm:ramda@0.23.0/src/internal/_isObject.js",
      "npm:ramda@0.23.0/src/internal/_isPlaceholder.js",
      "npm:ramda@0.23.0/src/internal/_isRegExp.js",
      "npm:ramda@0.23.0/src/internal/_isString.js",
      "npm:ramda@0.23.0/src/internal/_isTransformer.js",
      "npm:ramda@0.23.0/src/internal/_makeFlat.js",
      "npm:ramda@0.23.0/src/internal/_map.js",
      "npm:ramda@0.23.0/src/internal/_objectAssign.js",
      "npm:ramda@0.23.0/src/internal/_of.js",
      "npm:ramda@0.23.0/src/internal/_pipe.js",
      "npm:ramda@0.23.0/src/internal/_pipeP.js",
      "npm:ramda@0.23.0/src/internal/_quote.js",
      "npm:ramda@0.23.0/src/internal/_reduce.js",
      "npm:ramda@0.23.0/src/internal/_reduced.js",
      "npm:ramda@0.23.0/src/internal/_stepCat.js",
      "npm:ramda@0.23.0/src/internal/_toISOString.js",
      "npm:ramda@0.23.0/src/internal/_toString.js",
      "npm:ramda@0.23.0/src/internal/_xall.js",
      "npm:ramda@0.23.0/src/internal/_xany.js",
      "npm:ramda@0.23.0/src/internal/_xaperture.js",
      "npm:ramda@0.23.0/src/internal/_xchain.js",
      "npm:ramda@0.23.0/src/internal/_xdrop.js",
      "npm:ramda@0.23.0/src/internal/_xdropLast.js",
      "npm:ramda@0.23.0/src/internal/_xdropLastWhile.js",
      "npm:ramda@0.23.0/src/internal/_xdropRepeatsWith.js",
      "npm:ramda@0.23.0/src/internal/_xdropWhile.js",
      "npm:ramda@0.23.0/src/internal/_xfBase.js",
      "npm:ramda@0.23.0/src/internal/_xfilter.js",
      "npm:ramda@0.23.0/src/internal/_xfind.js",
      "npm:ramda@0.23.0/src/internal/_xfindIndex.js",
      "npm:ramda@0.23.0/src/internal/_xfindLast.js",
      "npm:ramda@0.23.0/src/internal/_xfindLastIndex.js",
      "npm:ramda@0.23.0/src/internal/_xmap.js",
      "npm:ramda@0.23.0/src/internal/_xreduceBy.js",
      "npm:ramda@0.23.0/src/internal/_xtake.js",
      "npm:ramda@0.23.0/src/internal/_xtakeWhile.js",
      "npm:ramda@0.23.0/src/internal/_xwrap.js",
      "npm:ramda@0.23.0/src/intersection.js",
      "npm:ramda@0.23.0/src/intersectionWith.js",
      "npm:ramda@0.23.0/src/intersperse.js",
      "npm:ramda@0.23.0/src/into.js",
      "npm:ramda@0.23.0/src/invert.js",
      "npm:ramda@0.23.0/src/invertObj.js",
      "npm:ramda@0.23.0/src/invoker.js",
      "npm:ramda@0.23.0/src/is.js",
      "npm:ramda@0.23.0/src/isArrayLike.js",
      "npm:ramda@0.23.0/src/isEmpty.js",
      "npm:ramda@0.23.0/src/isNil.js",
      "npm:ramda@0.23.0/src/join.js",
      "npm:ramda@0.23.0/src/juxt.js",
      "npm:ramda@0.23.0/src/keys.js",
      "npm:ramda@0.23.0/src/keysIn.js",
      "npm:ramda@0.23.0/src/last.js",
      "npm:ramda@0.23.0/src/lastIndexOf.js",
      "npm:ramda@0.23.0/src/length.js",
      "npm:ramda@0.23.0/src/lens.js",
      "npm:ramda@0.23.0/src/lensIndex.js",
      "npm:ramda@0.23.0/src/lensPath.js",
      "npm:ramda@0.23.0/src/lensProp.js",
      "npm:ramda@0.23.0/src/lift.js",
      "npm:ramda@0.23.0/src/liftN.js",
      "npm:ramda@0.23.0/src/lt.js",
      "npm:ramda@0.23.0/src/lte.js",
      "npm:ramda@0.23.0/src/map.js",
      "npm:ramda@0.23.0/src/mapAccum.js",
      "npm:ramda@0.23.0/src/mapAccumRight.js",
      "npm:ramda@0.23.0/src/mapObjIndexed.js",
      "npm:ramda@0.23.0/src/match.js",
      "npm:ramda@0.23.0/src/mathMod.js",
      "npm:ramda@0.23.0/src/max.js",
      "npm:ramda@0.23.0/src/maxBy.js",
      "npm:ramda@0.23.0/src/mean.js",
      "npm:ramda@0.23.0/src/median.js",
      "npm:ramda@0.23.0/src/memoize.js",
      "npm:ramda@0.23.0/src/merge.js",
      "npm:ramda@0.23.0/src/mergeAll.js",
      "npm:ramda@0.23.0/src/mergeWith.js",
      "npm:ramda@0.23.0/src/mergeWithKey.js",
      "npm:ramda@0.23.0/src/min.js",
      "npm:ramda@0.23.0/src/minBy.js",
      "npm:ramda@0.23.0/src/modulo.js",
      "npm:ramda@0.23.0/src/multiply.js",
      "npm:ramda@0.23.0/src/nAry.js",
      "npm:ramda@0.23.0/src/negate.js",
      "npm:ramda@0.23.0/src/none.js",
      "npm:ramda@0.23.0/src/not.js",
      "npm:ramda@0.23.0/src/nth.js",
      "npm:ramda@0.23.0/src/nthArg.js",
      "npm:ramda@0.23.0/src/objOf.js",
      "npm:ramda@0.23.0/src/of.js",
      "npm:ramda@0.23.0/src/omit.js",
      "npm:ramda@0.23.0/src/once.js",
      "npm:ramda@0.23.0/src/or.js",
      "npm:ramda@0.23.0/src/over.js",
      "npm:ramda@0.23.0/src/pair.js",
      "npm:ramda@0.23.0/src/partial.js",
      "npm:ramda@0.23.0/src/partialRight.js",
      "npm:ramda@0.23.0/src/partition.js",
      "npm:ramda@0.23.0/src/path.js",
      "npm:ramda@0.23.0/src/pathEq.js",
      "npm:ramda@0.23.0/src/pathOr.js",
      "npm:ramda@0.23.0/src/pathSatisfies.js",
      "npm:ramda@0.23.0/src/pick.js",
      "npm:ramda@0.23.0/src/pickAll.js",
      "npm:ramda@0.23.0/src/pickBy.js",
      "npm:ramda@0.23.0/src/pipe.js",
      "npm:ramda@0.23.0/src/pipeK.js",
      "npm:ramda@0.23.0/src/pipeP.js",
      "npm:ramda@0.23.0/src/pluck.js",
      "npm:ramda@0.23.0/src/prepend.js",
      "npm:ramda@0.23.0/src/product.js",
      "npm:ramda@0.23.0/src/project.js",
      "npm:ramda@0.23.0/src/prop.js",
      "npm:ramda@0.23.0/src/propEq.js",
      "npm:ramda@0.23.0/src/propIs.js",
      "npm:ramda@0.23.0/src/propOr.js",
      "npm:ramda@0.23.0/src/propSatisfies.js",
      "npm:ramda@0.23.0/src/props.js",
      "npm:ramda@0.23.0/src/range.js",
      "npm:ramda@0.23.0/src/reduce.js",
      "npm:ramda@0.23.0/src/reduceBy.js",
      "npm:ramda@0.23.0/src/reduceRight.js",
      "npm:ramda@0.23.0/src/reduceWhile.js",
      "npm:ramda@0.23.0/src/reduced.js",
      "npm:ramda@0.23.0/src/reject.js",
      "npm:ramda@0.23.0/src/remove.js",
      "npm:ramda@0.23.0/src/repeat.js",
      "npm:ramda@0.23.0/src/replace.js",
      "npm:ramda@0.23.0/src/reverse.js",
      "npm:ramda@0.23.0/src/scan.js",
      "npm:ramda@0.23.0/src/sequence.js",
      "npm:ramda@0.23.0/src/set.js",
      "npm:ramda@0.23.0/src/slice.js",
      "npm:ramda@0.23.0/src/sort.js",
      "npm:ramda@0.23.0/src/sortBy.js",
      "npm:ramda@0.23.0/src/sortWith.js",
      "npm:ramda@0.23.0/src/split.js",
      "npm:ramda@0.23.0/src/splitAt.js",
      "npm:ramda@0.23.0/src/splitEvery.js",
      "npm:ramda@0.23.0/src/splitWhen.js",
      "npm:ramda@0.23.0/src/subtract.js",
      "npm:ramda@0.23.0/src/sum.js",
      "npm:ramda@0.23.0/src/symmetricDifference.js",
      "npm:ramda@0.23.0/src/symmetricDifferenceWith.js",
      "npm:ramda@0.23.0/src/tail.js",
      "npm:ramda@0.23.0/src/take.js",
      "npm:ramda@0.23.0/src/takeLast.js",
      "npm:ramda@0.23.0/src/takeLastWhile.js",
      "npm:ramda@0.23.0/src/takeWhile.js",
      "npm:ramda@0.23.0/src/tap.js",
      "npm:ramda@0.23.0/src/test.js",
      "npm:ramda@0.23.0/src/times.js",
      "npm:ramda@0.23.0/src/toLower.js",
      "npm:ramda@0.23.0/src/toPairs.js",
      "npm:ramda@0.23.0/src/toPairsIn.js",
      "npm:ramda@0.23.0/src/toString.js",
      "npm:ramda@0.23.0/src/toUpper.js",
      "npm:ramda@0.23.0/src/transduce.js",
      "npm:ramda@0.23.0/src/transpose.js",
      "npm:ramda@0.23.0/src/traverse.js",
      "npm:ramda@0.23.0/src/trim.js",
      "npm:ramda@0.23.0/src/tryCatch.js",
      "npm:ramda@0.23.0/src/type.js",
      "npm:ramda@0.23.0/src/unapply.js",
      "npm:ramda@0.23.0/src/unary.js",
      "npm:ramda@0.23.0/src/uncurryN.js",
      "npm:ramda@0.23.0/src/unfold.js",
      "npm:ramda@0.23.0/src/union.js",
      "npm:ramda@0.23.0/src/unionWith.js",
      "npm:ramda@0.23.0/src/uniq.js",
      "npm:ramda@0.23.0/src/uniqBy.js",
      "npm:ramda@0.23.0/src/uniqWith.js",
      "npm:ramda@0.23.0/src/unless.js",
      "npm:ramda@0.23.0/src/unnest.js",
      "npm:ramda@0.23.0/src/until.js",
      "npm:ramda@0.23.0/src/update.js",
      "npm:ramda@0.23.0/src/useWith.js",
      "npm:ramda@0.23.0/src/values.js",
      "npm:ramda@0.23.0/src/valuesIn.js",
      "npm:ramda@0.23.0/src/view.js",
      "npm:ramda@0.23.0/src/when.js",
      "npm:ramda@0.23.0/src/where.js",
      "npm:ramda@0.23.0/src/whereEq.js",
      "npm:ramda@0.23.0/src/without.js",
      "npm:ramda@0.23.0/src/xprod.js",
      "npm:ramda@0.23.0/src/zip.js",
      "npm:ramda@0.23.0/src/zipObj.js",
      "npm:ramda@0.23.0/src/zipWith.js"
    ]
  },
  depCache: {
    "common/container.js": [
      "ramda"
    ],
    "common/i2b2.pub-sub.js": [
      "aurelia-framework",
      "aurelia-event-aggregator",
      "./i2b2.service",
      "./shrine.messages"
    ],
    "common/i2b2.service.js": [
      "ramda",
      "./container"
    ],
    "common/queries.model.js": [
      "aurelia-framework",
      "aurelia-event-aggregator",
      "repository/qep.repository",
      "./shrine.messages"
    ],
    "repository/qep.repository.js": [
      "aurelia-framework",
      "aurelia-fetch-client",
      "fetch"
    ],
    "shell.js": [
      "aurelia-framework",
      "aurelia-event-aggregator",
      "common/i2b2.pub-sub",
      "common/queries.model",
      "common/shrine.messages"
    ],
    "views/mailto/mailto.js": [
      "aurelia-framework",
      "views/mailto/mailto.service",
      "views/mailto/mailto.config"
    ],
    "views/mailto/mailto.service.js": [
      "aurelia-framework",
      "repository/qep.repository"
    ],
    "views/query-viewer/context-menu/context-menu.js": [
      "aurelia-framework",
      "aurelia-event-aggregator",
      "common/shrine.messages"
    ],
    "views/query-viewer/loading-bar/loading-bar.js": [
      "aurelia-framework"
    ],
    "views/query-viewer/query-viewer.js": [
      "aurelia-framework",
      "aurelia-event-aggregator",
      "common/queries.model",
      "./scroll.service",
      "common/shrine.messages"
    ],
    "views/query-viewer/query-viewer.service.js": [
      "aurelia-framework",
      "repository/qep.repository",
      "./query-viewer.config"
    ],
    "views/query-viewer/scroll.service.js": [
      "ramda",
      "common/container"
    ]
  }
});