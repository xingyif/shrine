"bundle";System.register("main.js",[],function(a,b){"use strict";function c(a){a.use.standardConfiguration().developmentLogging(),a.start().then(function(){return a.setRoot("shell")})}return a("configure",c),{setters:[],execute:function(){}}}),System.register("repository/pm-repository.js",[],function(a,b){"use strict";return{setters:[],execute:function(){}}}),function(){var a=System.amdDefine;a("shell.html!github:systemjs/plugin-text@0.0.8.js",[],function(){return"<template><!--compose view-model='views/mailto/mailto'>\n\t</compose--><div><compose view-model=\"views/query-viewer/query-viewer\"></compose></div></template>"})}(),System.register("shell.js",[],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d;return{setters:[],execute:function(){a("Shell",d=function b(){c(this,b)}),a("Shell",d)}}}),function(){var a=System.amdDefine;a("views/mailto/mailto.html!github:systemjs/plugin-text@0.0.8.js",[],function(){return'<template><div class="content mailto-hidden"><p>If you have questions about your query results or this SHRINE network, contact the Data Steward at your site.</p><div class="email js-email"><button class="button button--large js-button" click.delegate="openEmail()">Email Data Steward</button></div></div></template>'})}(),System.register("views/mailto/mailto.service.js",["aurelia-framework","aurelia-fetch-client","fetch"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h,i;return{setters:[function(a){d=a.inject},function(a){e=a.HttpClient},function(a){}],execute:function(){f=function(){function a(a,b){for(var c=0;c<b.length;c++){var d=b[c];d.enumerable=d.enumerable||!1,d.configurable=!0,"value"in d&&(d.writable=!0),Object.defineProperty(a,d.key,d)}}return function(b,c,d){return c&&a(b.prototype,c),d&&a(b,d),b}}(),a("MailToService",(g=d(e),i=g(h=function(){function a(b){var d=this;c(this,a),b.configure(function(a){a.useStandardConfiguration().withBaseUrl(d.url)}),this.http=b}return a.prototype.fetchStewardEmail=function(){return this.http.fetch("data?key=stewardEmail").then(function(a){return a.json()}).then(function(a){return a.indexOf('"')>0?a.split('"')[1]:a})["catch"](function(){return""})},f(a,[{key:"url",get:function(){var a="6443",b=document.URL,c="/shrine-metadata/";return b.substring(0,b.indexOf(a)+a.length)+c}}]),a}())||h)),a("MailToService",i)}}}),System.register("views/mailto/mailto.config.js",[],function(a,b){"use strict";var c;return{setters:[],execute:function(){a("MailConfig",c={mailto:"mailto:",subject:"subject="+encodeURIComponent("Question from a SHRINE User"),body:encodeURIComponent("Please enter the suggested information and your question. Your data steward will reply to this email.\n\n***Never send patient information, passwords, or other sensitive information by email****\nName:\nTitle:\nUser name (to log into SHRINE):\nTelephone Number (optional):\nPreferred email address (optional):\n\nQuestion or Comment:")}),a("MailConfig",c)}}}),System.register("views/mailto/mailto.js",["aurelia-framework","views/mailto/mailto.service","views/mailto/mailto.config"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h,i;return{setters:[function(a){d=a.inject},function(a){e=a.MailToService},function(a){f=a.MailConfig}],execute:function(){a("MailTo",(g=d(e,f),i=g(h=function(){function a(b,d){c(this,a),this.service=b,this.config=d}return a.prototype.openEmail=function(){var a=this;this.service.fetchStewardEmail().then(function(b){return window.top.location=a.getComposition(b),a})},a.prototype.getComposition=function(a){return this.config.mailto+a+"?"+this.config.subject+"&"+this.config.body},a}())||h)),a("MailTo",i)}}}),function(){var a=System.amdDefine;a("views/query-viewer/query-viewer.html!github:systemjs/plugin-text@0.0.8.js",[],function(){return'<template><div style="width: 100%; text-align: center"><ul class="circle-pagination"><li repeat.for="screen of screens" class="${screenIndex === $index? \'active\' : \'\'}" click.delegate="screenIndex = $index"><div>${screen.name}</div></li></ul></div><div class="box-wrapper"><div class="box" repeat.for="screen of screens" css="transform: translate(${slidePct}, 0%);"><section><!-- ${boxWrapper.offsetWidth} --><!--for demo wrap>\n                <!--h1>Fixed Table header</h1--><div><table cellpadding="0" cellspacing="0" border="0"><thead><tr><th class="tbl-header"></th><th repeat.for="node of screen.nodes" class="tbl-header"><div class="hideextra">${node}</div></th></tr></thead></table></div><div class="tbl-content"><table cellpadding="0" cellspacing="0" border="0"><tbody><tr repeat.for="query of screen.queries"><th><div class="hideextra">${query.name}</div></th><td repeat.for="result of query.results">${result.result}</td></tr></tbody></table></div></section></div></div></template>'})}(),System.register("views/query-viewer/query-viewer.service.js",["aurelia-framework","aurelia-fetch-client","fetch"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h,i,j;return{setters:[function(a){d=a.inject},function(a){e=a.HttpClient},function(a){}],execute:function(){f=function(){function a(a,b){for(var c=0;c<b.length;c++){var d=b[c];d.enumerable=d.enumerable||!1,d.configurable=!0,"value"in d&&(d.writable=!0),Object.defineProperty(a,d.key,d)}}return function(b,c,d){return c&&a(b.prototype,c),d&&a(b,d),b}}(),i=10,a("QueryViewerService",(g=d(e),j=g(h=function(){function a(b){var d=this;c(this,a),b.configure(function(a){a.useStandardConfiguration().withBaseUrl(d.url)}),this.http=b}return a.prototype.fetchPreviousQueries=function(){return this.http.fetch("previous-queries").then(function(a){return a.json()})["catch"](function(a){return a})},a.prototype.getNodes=function(a){return a.length>0?a[0].results.map(function(a){return a.node}):[]},a.prototype.getScreens=function(a,b){return new Promise(function(c,d){for(var e=a.length,f=[],g=function(c){var d=c+i<e?c+i:e-1,g=String(a[c]).substr(0,1)+"-"+String(a[d]).substr(0,1),h=a.slice(c,d),j=b.map(function(a){return{id:a.id,name:a.name,results:a.results.slice(c,d)}});f.push({name:g,nodes:h,queries:j})},h=0;h<e;h+=i)g(h);c(f)})},f(a,[{key:"url",get:function(){var a="8000",b=document.URL,c="6443/shrine-proxy/request/shrine/api/";return b.substring(0,b.indexOf(a))+c}}]),a}())||h)),a("QueryViewerService",j)}}}),System.register("views/query-viewer/query-viewer.js",["aurelia-framework","views/query-viewer/query-viewer.service"],function(a,b){"use strict";function c(a,b){if(!(a instanceof b))throw new TypeError("Cannot call a class as a function")}var d,e,f,g,h,i,j;return{setters:[function(a){d=a.inject},function(a){e=a.QueryViewerService}],execute:function(){f=function(){function a(a,b){for(var c=0;c<b.length;c++){var d=b[c];d.enumerable=d.enumerable||!1,d.configurable=!0,"value"in d&&(d.writable=!0),Object.defineProperty(a,d.key,d)}}return function(b,c,d){return c&&a(b.prototype,c),d&&a(b,d),b}}(),i=10,a("QueryViewer",(g=d(e),j=g(h=function(){function a(b){var d=this;c(this,a),this.screenIndex=0,this.service=b,this.service.fetchPreviousQueries().then(function(a){var c=a.queries,e=d.service.getNodes(c);return b.getScreens(e,c)}).then(function(a){d.screens=a})["catch"](function(a){return console.log(a)})}return f(a,[{key:"slidePct",get:function(){return String(-100*this.screenIndex)+"%"}}]),a}())||h)),a("QueryViewer",j)}}});