self.addEventListener("install", function(event) {
	event.waitUntil(preLoad());
});

const cachedFiles = [
	"/index.html",
	"/offline.html",
	"vuetify.min.css",
	"materialdesignicons.min.css",
	"vue.esm-browser.js",
	"/vuetify.esm.js",
	"/fonts/materialdesignicons-webfont.woff2?v=5.9.55",
	"/manifest.json",
	"/event.png"
];

var preLoad = function() {
	console.log("Installing web app");
	return caches.open("offline").then(function(cache) {
		console.log("caching index and important routes");
		return cache.addAll(cachedFiles);
	});
};

self.addEventListener("fetch", function(event) {
	event.respondWith(checkResponse(event.request).catch(function() {
		return returnFromCache(event.request);
	}));
	event.waitUntil(addToCache(event.request));
});

var checkResponse = function(request) {
	return new Promise(function(fulfill, reject) {
		fetch(request).then(function(response) {
			if (response.status !== 404) {
				fulfill(response);
			} else {
				reject();
			}
		}, reject);
	});
};

var addToCache = function(request) {
	return caches.open("offline").then(function(cache) {
		return fetch(request).then(function(response) {
			console.log(response.url + " was cached");
			return cache.put(request, response);
		});
	});
};

var returnFromCache = function(request) {
	return caches.open("offline").then(function(cache) {
		return cache.match(request).then(function(matching) {
			if (!matching || matching.status == 404) {
				return cache.match("offline.html");
			} else {
				return matching;
			}
		});
	});
};