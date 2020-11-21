function interceptNetworkRequests(ee) {
    const open = XMLHttpRequest.prototype.open;
    const send = XMLHttpRequest.prototype.send;

    const isRegularXHR = open.toString().indexOf('native code') !== -1;

    if (isRegularXHR) {
        XMLHttpRequest.prototype.open = function() {
            ee.onOpen && ee.onOpen(this, arguments);
            if (ee.onLoad) {
                this.addEventListener('load', ee.onLoad.bind(ee));
            }
            if (ee.onError) {
                this.addEventListener('error', ee.onError.bind(ee));
            }
            return open.apply(this, arguments);
        };
        XMLHttpRequest.prototype.send = function() {
            ee.onSend && ee.onSend(this, arguments);
            return send.apply(this, arguments);
        };
    }
    return ee;
}

var last;

function handleResult(action, r) {
    if(action != "onLoad") return;
    var urlToHandle = "https://translate.google.com/_/TranslateWebserverUi/data/batchexecute";
    if (r.target.responseURL && r.target.responseURL.startsWith(urlToHandle)) {
		var regResults = /(\[\["wrb\.fr",.*)/gm.exec(r.target.responseText);
		if (regResults.length >= 2) {
			var r1 = JSON.parse(regResults[0]+']');
			var r2 = r1[0][2];
			var r3 = JSON.parse(r2);
			var results = r3[1][0][0][5];
			var translated = "";
			results.forEach((item) => {
				translated += item[0];
			});
			try {
				var result = translated;
				last = r3;
				console.log("Translated", result, r3);
				window.MyJS.onTranslatedResult(result);
			} catch (error) {
			}
		}
    }
}

interceptNetworkRequests({
    onLoad: (r) => { handleResult("onLoad", r)}
});