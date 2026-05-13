/**
 * Simple IndexedDB wrapper for autosaving post drafts.
 */
window.DraftStore = (function () {
    var dbName = "SocialPublishDrafts";
    var dbVersion = 1;
    var storeName = "drafts";
    var db = null;

    return {
        init: function () {
            return new Promise(function (resolve, reject) {
                var request = indexedDB.open(dbName, dbVersion);
                request.onupgradeneeded = function (e) {
                    var db = e.target.result;
                    if (!db.objectStoreNames.contains(storeName)) {
                        db.createObjectStore(storeName);
                    }
                };
                request.onsuccess = function (e) {
                    db = e.target.result;
                    resolve();
                };
                request.onerror = function (e) { reject(e.target.error); };
            });
        },

        save: function (key, data) {
            return new Promise(function (resolve, reject) {
                if (!db) return reject("Database not initialized");
                var transaction = db.transaction([storeName], "readwrite");
                var store = transaction.objectStore(storeName);
                var request = store.put(data, key);
                request.onsuccess = function () { resolve(); };
                request.onerror = function (e) { reject(e.target.error); };
            });
        },

        load: function (key) {
            return new Promise(function (resolve, reject) {
                if (!db) return reject("Database not initialized");
                var transaction = db.transaction([storeName], "readonly");
                var store = transaction.objectStore(storeName);
                var request = store.get(key);
                request.onsuccess = function () { resolve(request.result); };
                request.onerror = function (e) { reject(e.target.error); };
            });
        },

        remove: function (key) {
            return new Promise(function (resolve, reject) {
                if (!db) return reject("Database not initialized");
                var transaction = db.transaction([storeName], "readwrite");
                var store = transaction.objectStore(storeName);
                var request = store.delete(key);
                request.onsuccess = function () { resolve(); };
                request.onerror = function (e) { reject(e.target.error); };
            });
        }
    };
})();

/**
 * Utility for debouncing functions.
 */
window.debounce = function (func, wait) {
    var timeout;
    var debounced = function () {
        var context = this, args = arguments;
        clearTimeout(timeout);
        timeout = setTimeout(function () {
            func.apply(context, args);
        }, wait);
    };
    debounced.cancel = function () {
        clearTimeout(timeout);
    };
    return debounced;
};
