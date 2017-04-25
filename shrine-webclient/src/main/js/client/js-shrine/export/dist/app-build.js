"bundle";System.register("main.js",[],function(a,b){"use strict";function c(a){a.use.standardConfiguration().developmentLogging(),a.start().then(function(){return a.setRoot("shell")})}return a("configure",c),{setters:[],execute:function(){}}}),function(){var a=System.amdDefine;a("shell.html!github:systemjs/plugin-text@0.0.8.js",[],function(){return"<template><router-view></router-view></template>"})}(),System.register("shell.js",[],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d;return{setters:[],execute:function(){a("Shell",d=function(){function a(){c(this,a)}return a.prototype.configureRouter=function(a,b){a.title="SHRINE Webclient Plugin",a.map([{route:"mailto",moduleId:"views/mailto/mailto"},{route:["","query-viewer"],moduleId:"views/query-viewer/query-viewer"}]),this.router=b},a}()),a("Shell",d)}}}),function(){var a=System.amdDefine;a("views/mailto/mailto.html!github:systemjs/plugin-text@0.0.8.js",[],function(){return'<template><div class="mailto"><div class="content"><p>If you have questions about your query results or this SHRINE network, contact the Data Steward at your site.</p><div class="email js-email"><button class="button button--large js-button" click.delegate="openEmail()">Email Data Steward</button></div></div></div></template>'})}(),System.register("views/mailto/mailto.service.js",["aurelia-framework","repository/qep.repository"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h;return{setters:[function(a){d=a.inject},function(a){e=a.QEPRepository}],execute:function(){a("MailToService",(f=d(e),h=f(g=function(){function a(b){c(this,a),this.repository=b}return a.prototype.fetchStewardEmail=function(){return this.repository.fetchStewardEmail()},a}())||g)),a("MailToService",h)}}}),System.register("views/mailto/mailto.config.js",[],function(a,b){"use strict";var c;return{setters:[],execute:function(){a("MailConfig",c={mailto:"mailto:",subject:"subject="+encodeURIComponent("Question from a SHRINE User"),body:encodeURIComponent("Please enter the suggested information and your question. Your data steward will reply to this email.\n\n***Never send patient information, passwords, or other sensitive information by email****\nName:\nTitle:\nUser name (to log into SHRINE):\nTelephone Number (optional):\nPreferred email address (optional):\n\nQuestion or Comment:")}),a("MailConfig",c)}}}),System.register("views/mailto/mailto.js",["aurelia-framework","views/mailto/mailto.service","views/mailto/mailto.config"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h,i;return{setters:[function(a){d=a.inject},function(a){e=a.MailToService},function(a){f=a.MailConfig}],execute:function(){a("MailTo",(g=d(e,f),i=g(h=function(){function a(b,d){c(this,a),this.service=b,this.config=d}return a.prototype.openEmail=function(){var a=this;this.service.fetchStewardEmail().then(function(b){return window.top.location=a.getComposition(b),a})},a.prototype.getComposition=function(a){return this.config.mailto+a+"?"+this.config.subject+"&"+this.config.body},a}())||h)),a("MailTo",i)}}}),System.register("views/query-viewer/box-style.converter.js",[],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d;return{setters:[],execute:function(){a("BoxStyleValueConverter",d=function(){function a(){c(this,a)}return a.prototype.toView=function(a){return"transform: translate("+String(-100*a)+"%);"},a}()),a("BoxStyleValueConverter",d)}}}),function(){var a=System.amdDefine;a("views/query-viewer/query-viewer.html!github:systemjs/plugin-text@0.0.8.js",[],function(){return'<template><require from="views/query-viewer/result-style.converter"></require><require from="views/query-viewer/result-value.converter"></require><require from="views/query-viewer/box-style.converter"></require><div style="width: 100%; text-align: center"><ul class="circle-pagination" if.bind="showCircles"><li repeat.for="screen of screens" class="${screenIndex === $index? \'active\' : \'\'}" click.delegate="screenIndex = $index"><div>${screen.id}</div></li></ul></div><div class="box-wrapper"><div class="box" repeat.for="screen of screens" css.bind="screenIndex | boxStyle"><section><div><table cellpadding="0" cellspacing="0" border="0"><thead><tr><th class="tbl-header"></th><th repeat.for="node of screen.nodes" class="tbl-header"><div class="hideextra">${node}</div></th></tr></thead></table></div><div class="tbl-content ${vertStyle}"><table cellpadding="0" cellspacing="0" border="0"><tbody><tr repeat.for="result of screen.results" css="background: ${$odd? \'rgba(98,108,146, .1)\' : \'white\'}"><th><div class="hideextra">${result.name}</div></th><td repeat.for="nodeResult of result.nodeResults" css="${nodeResult | resultStyle}">${nodeResult | result}</td></tr></tbody></table></div></section></div></div></template>'})}(),System.register("repository/qep.repository.js",["aurelia-framework","aurelia-fetch-client","fetch"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h,i;return{setters:[function(a){d=a.inject},function(a){e=a.HttpClient},function(a){}],execute:function(){f=function(){function a(a,b){for(var c=0;c<b.length;c++){var d=b[c];d.enumerable=d.enumerable||!1,d.configurable=!0,"value"in d&&(d.writable=!0),Object.defineProperty(a,d.key,d)}}return function(b,c,d){return c&&a(b.prototype,c),d&&a(b,d),b}}(),a("QEPRepository",(g=d(e),i=g(h=function(){function a(b){var d=this;c(this,a),b.configure(function(a){a.useStandardConfiguration().withBaseUrl(d.url).withDefaults({headers:{Authorization:"Basic "+d.auth}})}),this.http=b}return a.prototype.fetchPreviousQueries=function(){return this.http.fetch("qep/queryResults").then(function(a){return a.json()})["catch"](function(a){return a})},a.prototype.fetchStewardEmail=function(){return this.http.fetch("data?key=stewardEmail").then(function(a){return a.json()}).then(function(a){return a.indexOf('"')>0?a.split('"')[1]:a})["catch"](function(){return""})},f(a,[{key:"url",get:function(){var a=document.URL,b=":6443/shrine-metadata/";return a.substring(0,a.lastIndexOf(":"))+b}},{key:"auth",get:function(){var a=sessionStorage.getItem("shrine.auth");return sessionStorage.removeItem("shrine.auth"),a}}]),a}())||h)),a("QEPRepository",i)}}}),System.register("views/query-viewer/query-viewer.config.js",[],function(a,b){"use strict";var c;return{setters:[],execute:function(){a("QueryViewerConfig",c={maxNodesPerScreen:10}),a("QueryViewerConfig",c)}}}),System.register("views/query-viewer/query-viewer.service.js",["aurelia-framework","repository/qep.repository","./query-viewer.config"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h,i;return{setters:[function(a){d=a.inject},function(a){e=a.QEPRepository},function(a){f=a.QueryViewerConfig}],execute:function(){a("QueryViewerService",(g=d(e,f),i=g(h=function(){function a(b,d,e){c(this,a),this.repository=b,this.config=d}return a.prototype.fetchPreviousQueries=function(){return this.repository.fetchPreviousQueries()},a.prototype.getScreens=function(a,b){var c=this;return new Promise(function(d,e){for(var f=a.sort().length,g=[],h=0;h<f;h+=c.config.maxNodesPerScreen){var i=c.getNumberOfNodesOnScreen(a,h,c.config.maxNodesPerScreen),j=i-1,k=c.getScreenId(a,h,j),l=a.slice(h,i),m=c.mapQueriesToScreenNodes(l,b,c.findQueriesForNode);g.push({id:k,nodes:l,results:m})}d(g)})},a.prototype.mapQueriesToScreenNodes=function(a,b){var c=[];return b.forEach(function(b,d){var e={name:b.query.queryName,id:b.query.networkId,nodeResults:[]};a.forEach(function(a){e.nodeResults.push(b.adaptersToResults.find(function(b){return b.adapterNode===a}))}),c.push(e)}),c},a.prototype.getNumberOfNodesOnScreen=function(a,b){var c=b+this.config.maxNodesPerScreen;return c<a.length?c:a.length},a.prototype.getScreenId=function(a,b,c){var d=a[b],e=a[c];return String(d).substr(0,1)+"-"+String(e).substr(0,1)},a}())||h)),a("QueryViewerService",i)}}}),System.register("common/i2b2.service.js",[],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g;return{setters:[],execute:function(){a("I2B2Service",d=function b(){var a=arguments.length>0&&void 0!==arguments[0]?arguments[0]:window;c(this,b);var d=e(a,"i2b2");this.onResize=function(a){return d?d.events.changedZoomWindows.subscribe(a):null},this.onHistory=function(a){return d?d.CRC.ctrlr.history.events.onDataUpdate.subscribe(a):null}}),a("I2B2Service",d),e=function(a,b){return g(a)?f(a)[b]:null},f=function(a){return a.parent.window},g=function(a){return a&&a.parent&&a.parent.window}}}}),System.register("views/query-viewer/query-viewer.model.js",[],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d;return{setters:[],execute:function(){a("QueryViewerModel",d=function b(){c(this,b),this.isLoaded=!1,this.screens=[]}),a("QueryViewerModel",d)}}}),System.register("views/query-viewer/query-viewer.js",["aurelia-framework","views/query-viewer/query-viewer.service","common/i2b2.service.js","./query-viewer.model"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h,i,j,k;return{setters:[function(a){d=a.inject,e=a.computedFrom},function(a){f=a.QueryViewerService},function(a){g=a.I2B2Service},function(a){h=a.QueryViewerModel}],execute:function(){a("QueryViewer",(i=d(f,g,h),k=i(j=function b(a,d,e){var f=this;c(this,b),this.screenIndex=0,this.showCircles=!1,this.service=a,this.vertStyle="v-min";var g=function(a){return f.service.getScreens(a.adapters,a.queryResults)},h=function(a){f.screens=a,f.showCircles=f.screens.length>1,e.screens=a,e.isLoaded=!0},i=function(){return f.service.fetchPreviousQueries().then(g).then(h)["catch"](function(a){return console.log(a)})},j=function(){return e.isLoaded?h(e.screens):i()},k=function(a){return"ADD"!==a.action};d.onResize(function(a,b){return f.vertStyle=b.find(k)?"v-min":"v-full"}),d.onHistory(i),j()})||j)),a("QueryViewer",k)}}}),System.register("views/query-viewer/result-style.converter.js",[],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d;return{setters:[],execute:function(){a("ResultStyleValueConverter",d=function(){function a(){c(this,a)}return a.prototype.toView=function(a){var b=this.isUnresolved(a)?"color:"+this.getColorValue(a):"";return b},a.prototype.isUnresolved=function(a){var b=arguments.length>1&&void 0!==arguments[1]?arguments[1]:"FINISHED";return!a||a.status!==b},a.prototype.getColorValue=function(a){var b=arguments.length>1&&void 0!==arguments[1]?arguments[1]:"ERROR",c=arguments.length>2&&void 0!==arguments[2]?arguments[2]:"#FF0000",d=arguments.length>3&&void 0!==arguments[3]?arguments[3]:"#00FF00";return a&&a.status!==b?d:c},a}()),a("ResultStyleValueConverter",d)}}}),System.register("views/query-viewer/result-value.converter.js",[],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d;return{setters:[],execute:function(){a("ResultValueConverter",d=function(){function a(){c(this,a)}return a.prototype.toView=function(a){return a?"FINISHED"!==a.status?a.status:a.count<0?"<=10":a.count:"not available"},a}()),a("ResultValueConverter",d)}}});