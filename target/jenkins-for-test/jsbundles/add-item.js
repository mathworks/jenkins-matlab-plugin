(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
var internal = require("./internal");
var promise = require("./promise");
var onRegisterTimeout;
var whoami;

/**
 * What's the top level module/bundle name.
 * @param moduleQName The module QName.
 * @returns The module QName, or undefined if unknown.
 */
exports.whoami = function(moduleQName) {
    if (moduleQName) {
        whoami = moduleQName;
        internal.whoami(whoami);
    }
    return whoami;
};

/**
 * Asynchronously import/require a set of modules.
 *
 * <p>
 * Responsible for triggering the async loading of modules if a given module is not already loaded.
 *
 * @param moduleQNames... A list of module "qualified" names, each containing the module name prefixed with the namespace
 * and separated by a colon i.e. "<namespace>:<moduleName>" e.g. "jquery:jquery2".
 *
 * @return A Promise, allowing async load of all modules. The promise is only fulfilled when all modules are loaded.
 */
exports.import = function() {
    if (arguments.length === 1) {
        return internal.import(arguments[0], onRegisterTimeout);        
    }
    
    var moduleQNames = [];    
    for (var i = 0; i < arguments.length; i++) {
        var argument = arguments[i];
        if (typeof argument === 'string') {
            moduleQNames.push(argument);
        }
    }
    
    if (moduleQNames.length == 0) {
        throw "No module names specified.";
    }
    
    return promise.make(function (resolve, reject) {
        var fulfillments = [];
        
        function onFulfillment() {
            if (fulfillments.length === moduleQNames.length) {
                var modules = [];
                for (var i = 0; i < fulfillments.length; i++) {
                    if (fulfillments[i].value) {
                        modules.push(fulfillments[i].value);
                    } else {
                        // don't have everything yet so can't fulfill all.
                        return;
                    }
                }
                // If we make it here, then we have fulfilled all individual promises, which 
                // means we can now fulfill the top level import promise.
                resolve(modules);
            }
        }        
        
        // doRequire for each module
        for (var i = 0; i < moduleQNames.length; i++) {           
            function doRequire(moduleQName) {
                var promise = internal.import(moduleQName, onRegisterTimeout);
                var fulfillment = {
                    promise: promise,
                    value: undefined
                };
                fulfillments.push(fulfillment);
                promise
                    .onFulfilled(function(value) {
                        fulfillment.value = value;
                        onFulfillment();
                    })
                    .onRejected(function(error) {
                        reject(error);
                    });
            }
            doRequire(moduleQNames[i]);
        }
    }).applyArgsOnFulfill();    
};

/**
 * Synchronously "require" a module that it already loaded/registered.
 *
 * <p>
 * This function will throw an error if the module is not already loaded via an outer call to 'import'
 * (or 'import').
 *
 * @param moduleQName The module "qualified" name containing the module name prefixed with the namespace
 * separated by a colon i.e. "<namespace>:<moduleName>" e.g. "jquery:jquery2".
 *
 * @return The module.
 */
exports.require = function(moduleQName) {
    var parsedModuleName = internal.parseResourceQName(moduleQName);
    var module = internal.getModule(parsedModuleName);    
    if (!module) {
        throw "Unable to perform synchronous 'require' for module '" + moduleQName + "'. This module is not pre-loaded. " +
            "The module needs to have been asynchronously pre-loaded via an outer call to 'import'.";
    }
    return module.exports;
}

/**
 * Export a module.
 * 
 * @param namespace The namespace in which the module resides, or "undefined" if the modules is in
 * the "global" module namespace e.g. a Jenkins core bundle.
 * @param moduleName The name of the module. 
 * @param module The CommonJS style module, or "undefined" if we just want to notify other modules waiting on
 * the loading of this module.
 * @param onError On error callback;
 */
exports.export = function(namespace, moduleName, module, onError) {
    internal.onReady(function() {
        try {
            var moduleSpec = {namespace: namespace, moduleName: moduleName};
            var moduleNamespaceObj = internal.getModuleNamespaceObj(moduleSpec);
            
            if (moduleNamespaceObj[moduleName]) {
                if (namespace) {
                    throw "Jenkins plugin module '" + namespace + ":" + moduleName + "' already registered.";
                } else {
                    throw "Jenkins global module '" + moduleName + "' already registered.";
                }
            }
            
            if (!module) {
                module = {
                    exports: {}
                };
            } else if (module.exports === undefined) {
                module = {
                    exports: module
                };
            }
            moduleNamespaceObj[moduleName] = module;
            
            // Notify all that the module has been registered. See internal.loadModule also.
            internal.notifyModuleExported(moduleSpec, module.exports);
        } catch (e) {
            console.error(e);
            if (onError) {
                onError(e);
            }
        }
    });
};

/**
 * Add a module's CSS to the browser page.
 * 
 * <p>
 * The assumption is that the CSS can be accessed at e.g.
 * {@code <rootURL>/plugin/<namespace>/jsmodules/<moduleName>/style.css} i.e.
 * the pluginId acts as the namespace.
 * 
 * @param namespace The namespace in which the module resides.
 * @param moduleName The name of the module. 
 * @param onError On error callback;
 */
exports.addModuleCSSToPage = function(namespace, moduleName, onError) {
    internal.onReady(function() {
        try {
            internal.addModuleCSSToPage(namespace, moduleName);
        } catch (e) {
            console.error(e);
            if (onError) {
                onError(e);
            }
        }
    });
};

/**
 * Add a plugin CSS file to the browser page.
 * 
 * @param pluginName The Jenkins plugin in which the module resides.
 * @param cssPath The CSS path. 
 * @param onError On error callback;
 */
exports.addPluginCSSToPage = function(pluginName, cssPath, onError) {
    internal.onReady(function() {
        try {
            internal.addPluginCSSToPage(pluginName, cssPath);
        } catch (e) {
            console.error(e);
            if (onError) {
                onError(e);
            }
        }
    });
};

/**
 * Add CSS file to the browser page.
 * 
 * @param cssPath The CSS path. 
 * @param onError On error callback;
 */
exports.addCSSToPage = function(cssPath, onError) {
    internal.onReady(function() {
        try {           
            internal.addCSSToPage('global', internal.getRootURL() + '/' + cssPath);
        } catch (e) {
            console.error(e);
            if (onError) {
                onError(e);
            }
        }
    });
};

/**
 * Add a javascript &lt;script&gt; element to the document &lt;head&gt;.
 * <p/>
 * Options:
 * <ul>
 *     <li><strong>scriptId</strong>: The script Id to use for the element. If not specified, one will be generated from the scriptSrc.</li>
 *     <li><strong>async</strong>: Asynchronous loading of the script. Default is 'true'.</li>
 *     <li><strong>success</strong>: An optional onload success function for the script element.</li>
 *     <li><strong>error</strong>: An optional onload error function for the script element. This is called if the .js file exists but there's an error evaluating the script. It is NOT called if the .js file doesn't exist (ala 404).</li>
 *     <li><strong>removeElementOnLoad</strong>: Remove the script element after loading the script. Default is 'false'.</li>
 * </ul>
 * 
 * @param scriptSrc The script src.
 * @param options Optional script load options object. See above.
 */
exports.addScript = function(scriptSrc, options) {
    internal.onReady(function() {
        internal.addScript(scriptSrc, options);
    });    
};

/**
 * Set the module registration timeout i.e. the length of time to wait for a module to load before failing.
 *
 * @param timeout Millisecond duration before onRegister times out. Defaults to 10000 (10s) if not specified.
 */
exports.setRegisterTimeout = function(timeout) {
    onRegisterTimeout = timeout;
}

/**
 * Set the Jenkins root/base URL.
 * 
 * @param rootUrl The root/base URL.
 */
exports.setRootURL = function(rootUrl) {
    internal.setRootURL(rootUrl);
};

exports.getRootURL = internal.getRootURL;
exports.getAdjunctURL = internal.getAdjunctURL;

/**
 * Manually initialise the Jenkins Global.
 * <p>
 * This should only ever be called from a test environment.
 */
exports.initJenkinsGlobal = function() {
    internal.initJenkinsGlobal();
};

internal.onJenkinsGlobalInit(function(jenkinsCIGlobal) {
    // For backward compatibility, we need to make some jenkins-js-modules
    // functions globally available e.g. to allow legacy code wait for
    // certain modules to be loaded, as with legacy adjuncts.
    if (!jenkinsCIGlobal._internal) {
        // Put the functions on an object called '_internal' as a way
        // of hinting to people to not use it.
        jenkinsCIGlobal._internal = {
            import: exports.import,
            addScript: internal.addScript
        };
    }
});
},{"./internal":2,"./promise":3}],2:[function(require,module,exports){
var promise = require("./promise");
var windowHandle = require("window-handle");
var jenkinsCIGlobal;
var globalInitListeners = [];
var whoami;

exports.whoami = function(moduleQName) {
    if (moduleQName) {
        whoami = exports.parseResourceQName(moduleQName);
        whoami.nsProvider = getBundleNSProviderFromScriptElement(whoami.namespace, whoami.moduleName);
    }
    return whoami;
};

exports.onReady = function(callback) {
    // This allows test based initialization of jenkins-js-modules when there might 
    // not yet be a global window object.
    if (jenkinsCIGlobal) {
        callback();
    } else {
        windowHandle.getWindow(function() {
            callback();
        });
    }    
};

exports.onJenkinsGlobalInit = function(callback) {
    globalInitListeners.push(callback);
};

exports.initJenkinsGlobal = function() {
    jenkinsCIGlobal = {
    };
    if (globalInitListeners) {
        for (var i = 0; i < globalInitListeners.length; i++) {
            globalInitListeners[i](jenkinsCIGlobal);
        }
    }
};

exports.clearJenkinsGlobal = function() {    
    jenkinsCIGlobal = undefined;
    whoami = undefined;
};

exports.getJenkins = function() {
    if (jenkinsCIGlobal) {
        return jenkinsCIGlobal;
    }
    var window = windowHandle.getWindow();
    if (window.jenkinsCIGlobal) {
        jenkinsCIGlobal = window.jenkinsCIGlobal;
    } else {
        exports.initJenkinsGlobal();
        jenkinsCIGlobal.rootURL = getRootURL();
        window.jenkinsCIGlobal = jenkinsCIGlobal;
    }   
    return jenkinsCIGlobal;
};

exports.getModuleNamespaceObj = function(moduleSpec) {
    if (moduleSpec.namespace) {
        return exports.getNamespace(moduleSpec.namespace);
    } else {
        return exports.getGlobalModules();
    }
}

exports.getNamespace = function(namespaceName) {
    var namespaces = exports.getNamespaces();
    var namespace = namespaces[namespaceName];
    if (!namespace) {
        namespace = {
            globalNS: false            
        };
        namespaces[namespaceName] = namespace;
    }
    return namespace;
};

exports.import = function(moduleQName, onRegisterTimeout) {
    return promise.make(function (resolve, reject) {
        // Some functions here needs to access the 'window' global. We want to make sure that
        // exists before attempting to fulfill the require operation. It may not exists
        // immediately in a test env.
        exports.onReady(function() {
            var moduleSpec = exports.parseResourceQName(moduleQName);
            var module = exports.getModule(moduleSpec);
            
            if (module) {
                // module already loaded
                resolve(module.exports);
            } else {
                if (onRegisterTimeout === 0) {
                    if (moduleSpec.namespace) {
                        throw 'Module ' + moduleSpec.namespace + ':' + moduleSpec.moduleName + ' require failure. Async load mode disabled.';
                    } else {
                        throw 'Global module ' + moduleSpec.moduleName + ' require failure. Async load mode disabled.';
                    }
                }

                // module not loaded. Load async, fulfilling promise once registered
                exports.loadModule(moduleSpec, onRegisterTimeout)
                    .onFulfilled(function (moduleExports) {
                        resolve(moduleExports);
                    })
                    .onRejected(function (error) {
                        reject(error);
                    });
            }
        });
    });    
};

exports.loadModule = function(moduleSpec, onRegisterTimeout) {
    var moduleNamespaceObj = exports.getModuleNamespaceObj(moduleSpec);
    var module = moduleNamespaceObj[moduleSpec.moduleName];
    
    if (module) {
        // Module already loaded. This prob shouldn't happen.
        console.log("Unexpected call to 'loadModule' for a module (" + moduleSpec.moduleName + ") that's already loaded.");
        return promise.make(function (resolve) {
            resolve(module.exports);
        });
    }

    function waitForRegistration(loadingModule, onRegisterTimeout) {
        return promise.make(function (resolve, reject) {
            if (typeof onRegisterTimeout !== "number") {
                onRegisterTimeout = 10000;
            }
            
            var timeoutObj = setTimeout(function () {
                // Timed out waiting on the module to load and register itself.
                if (!loadingModule.loaded) {
                    var moduleSpec = loadingModule.moduleSpec;
                    var errorDetail;
                    
                    if (moduleSpec.namespace) {
                        errorDetail = "Timed out waiting on module '" + moduleSpec.namespace + ":" + moduleSpec.moduleName + "' to load.";
                    } else {
                        errorDetail = "Timed out waiting on global module '" + moduleSpec.moduleName + "' to load.";
                    }                    
                    console.error('Module load failure: ' + errorDetail);

                    // Call the reject function and tell it we timed out
                    reject({
                        reason: 'timeout',
                        detail: errorDetail
                    });
                }
            }, onRegisterTimeout);
            
            loadingModule.waitList.push({
                resolve: resolve,
                timeoutObj: timeoutObj
            });                    
        });
    }
    
    var loadingModule = getLoadingModule(moduleNamespaceObj, moduleSpec.moduleName);
    if (!loadingModule.waitList) {
        loadingModule.waitList = [];
    }
    loadingModule.moduleSpec = moduleSpec; 
    loadingModule.loaded = false;

    try {
        return waitForRegistration(loadingModule, onRegisterTimeout);
    } finally {
        // We can auto/dynamic load modules in a non-global namespace. Global namespace modules
        // need to make sure they load themselves (via an adjunct, or whatever).
        if (moduleSpec.namespace) {
            var scriptId = exports.toModuleId(moduleSpec.namespace, moduleSpec.moduleName) + ':js';
            var scriptSrc = exports.toModuleSrc(moduleSpec, 'js');
            var scriptEl = exports.addScript(scriptSrc, {
                scriptId: scriptId,
                scriptSrcBase: ''
            });

            if (scriptEl) {
                // Set the module spec info on the <script> element. This allows us to resolve the
                // nsProvider for that bundle after 'whoami' is called for it (as it loads). whoami
                // is not called with the nsProvider info on it because a given bundle can
                // potentially be loaded from multiple different ns providers, so we only resole the provider
                // at load-time i.e. just after a bundle is loaded it calls 'whoami' for itself
                // and then this module magically works out where it was loaded from (it's nsProvider)
                // by locating the <script> element and using this information. For a module/bundle, knowing
                // where it was loaded from is important because it dictates where that module/bundle
                // should load it dependencies from. For example, the Bootstrap module/bundle depends on the
                // jQuery bundle. So, if the bootstrap bundle is loaded from the 'core-assets' namespace provider,
                // then that means the jQuery bundle should also be loaded from the 'core-assets'
                // namespace provider.
                // See getBundleNSProviderFromScriptElement.
                scriptEl.setAttribute('data-jenkins-module-nsProvider', moduleSpec.nsProvider);
                scriptEl.setAttribute('data-jenkins-module-namespace', moduleSpec.namespace);
                scriptEl.setAttribute('data-jenkins-module-moduleName', moduleSpec.moduleName);
            }
        }
    }
};

exports.addScript = function(scriptSrc, options) {
    if (!scriptSrc) {
        console.warn('Call to addScript with undefined "scriptSrc" arg.');
        return undefined;
    }    
    
    var normalizedOptions;
    
    // If there's no options object, create it.
    if (typeof options === 'object') {
        normalizedOptions = options;
    } else {
        normalizedOptions = {};
    }
    
    // May want to transform/map some urls.
    if (normalizedOptions.scriptSrcMap) {
        if (typeof normalizedOptions.scriptSrcMap === 'function') {
            scriptSrc = normalizedOptions.scriptSrcMap(scriptSrc);
        } else if (Array.isArray(normalizedOptions.scriptSrcMap)) {
            // it's an array of suffix mappings
            for (var i = 0; i < normalizedOptions.scriptSrcMap.length; i++) {
                var mapping = normalizedOptions.scriptSrcMap[i];
                if (mapping.from && mapping.to) {
                    if (endsWith(scriptSrc, mapping.from)) {
                        normalizedOptions.originalScriptSrc = scriptSrc;
                        scriptSrc = scriptSrc.replace(mapping.from, mapping.to);
                        break;
                    }
                }
            }
        }
    }
    
    normalizedOptions.scriptId = getScriptId(scriptSrc, options);
    
    // set some default options
    if (normalizedOptions.async === undefined) {
        normalizedOptions.async = true;
    }
    if (normalizedOptions.scriptSrcBase === undefined) {
        normalizedOptions.scriptSrcBase = '@root';
    }
    
    if (normalizedOptions.scriptSrcBase === '@root') {
        normalizedOptions.scriptSrcBase = getRootURL() + '/';
    } else if (normalizedOptions.scriptSrcBase === '@adjunct') {
        normalizedOptions.scriptSrcBase = getAdjunctURL() + '/';
    }

    var document = windowHandle.getWindow().document;
    var head = exports.getHeadElement();
    var script = document.getElementById(normalizedOptions.scriptId);

    if (script) {
        var replaceable = script.getAttribute('data-replaceable');
        if (replaceable && replaceable === 'true') {
            // This <script> element is replaceable. In this case, 
            // we remove the existing script element and add a new one of the
            // same id and with the specified src attribute.
            // Adding happens below.
            script.parentNode.removeChild(script);
        } else {
            return undefined;
        }
    }

    script = createElement('script');

    // Parts of the following onload code were inspired by how the ACE editor does it,
    // as well as from the follow SO post: http://stackoverflow.com/a/4845802/1166986
    var onload = function (_, isAborted) {
        script.setAttribute('data-onload-complete', true);
        try {
            if (isAborted) {
                console.warn('Script load aborted: ' + scriptSrc);
            } else if (!script.readyState || script.readyState === "loaded" || script.readyState === "complete") {
                // If the options contains an onload function, call it.
                if (typeof normalizedOptions.success === 'function') {
                    normalizedOptions.success(script);
                }
                return;
            }
            if (typeof normalizedOptions.error === 'function') {
                normalizedOptions.error(script, isAborted);
            }
        } finally {
            if (normalizedOptions.removeElementOnLoad) {
                head.removeChild(script);
            }
            // Handle memory leak in IE
            script = script.onload = script.onreadystatechange = null;
        }
    };
    script.onload = onload; 
    script.onreadystatechange = onload;

    script.setAttribute('id', normalizedOptions.scriptId);
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', normalizedOptions.scriptSrcBase + scriptSrc);
    if (normalizedOptions.originalScriptSrc) {
        script.setAttribute('data-referrer', normalizedOptions.originalScriptSrc);        
    }
    if (normalizedOptions.async) {
        script.setAttribute('async', normalizedOptions.async);
    }
    
    head.appendChild(script);
    
    return script;
};

exports.notifyModuleExported = function(moduleSpec, moduleExports) {
    var moduleNamespaceObj = exports.getModuleNamespaceObj(moduleSpec);
    var loadingModule = getLoadingModule(moduleNamespaceObj, moduleSpec.moduleName);
    
    loadingModule.loaded = true;
    if (loadingModule.waitList) {
        for (var i = 0; i < loadingModule.waitList.length; i++) {
            var waiter = loadingModule.waitList[i];
            clearTimeout(waiter.timeoutObj);
            waiter.resolve(moduleExports);
        }
    }    
};

exports.addModuleCSSToPage = function(namespace, moduleName) {
    var moduleSpec = exports.getModuleSpec(namespace + ':' + moduleName);
    var cssElId = exports.toModuleId(namespace, moduleName) + ':css';
    var cssPath = exports.toModuleSrc(moduleSpec, 'css');
    return exports.addCSSToPage(namespace, cssPath, cssElId);
};

exports.addPluginCSSToPage = function(namespace, cssPath, cssElId) {
    var cssPath = exports.getPluginPath(namespace) + '/' + cssPath;
    return exports.addCSSToPage(namespace, cssPath, cssElId);
};

exports.addCSSToPage = function(namespace, cssPath, cssElId) {
    var document = windowHandle.getWindow().document;
    
    if (cssElId === undefined) {
        cssElId = 'jenkins-js-module:' + namespace + ':css:' + cssPath;
    }
    
    var cssEl = document.getElementById(cssElId);
    
    if (cssEl) {
        // already added to page
        return;
    }

    var docHead = exports.getHeadElement();
    cssEl = createElement('link');
    cssEl.setAttribute('id', cssElId);
    cssEl.setAttribute('type', 'text/css');
    cssEl.setAttribute('rel', 'stylesheet');
    cssEl.setAttribute('href', cssPath);
    docHead.appendChild(cssEl);

    return cssEl;
};

exports.getGlobalModules = function() {
    var jenkinsCIGlobal = exports.getJenkins();
    if (!jenkinsCIGlobal.globals) {
        jenkinsCIGlobal.globals = {
            globalNS: true
        };
    }
    return jenkinsCIGlobal.globals;
};

exports.getNamespaces = function() {
    var jenkinsCIGlobal = exports.getJenkins();

    // The namespaces are stored in an object named "plugins". This is a legacy from the
    // time when all modules lived in plugins. By right we'd like to rename this, but
    // that would cause compatibility issues.

    if (!jenkinsCIGlobal.plugins) {
        jenkinsCIGlobal.plugins = {
            __README__: 'This object holds namespaced JS modules/bundles, with the property names representing the module namespace. It\'s name ("plugins") is a legacy thing. Changing it to a better name (e.g. "namespaces") would cause compatibility issues.'
        };
    }
    return jenkinsCIGlobal.plugins;
};

exports.toModuleId = function(namespace, moduleName) {
    return 'jenkins-js-module:' + namespace + ':' + moduleName;
};

exports.toModuleSrc = function(moduleSpec, srcType) {
    var nsProvider = moduleSpec.nsProvider;

    // If a moduleSpec on a module/bundle import doesn't specify a namespace provider
    // (i.e. is of the form "a:b" and not "core-assets/a:b"),
    // then check "this" bundles module spec and see if it was imported from a specific
    // namespace. If it was (e.g. 'core-assets'), then import from that namespace.
    if (nsProvider === undefined) {
        nsProvider = thisBundleNamespaceProvider();
        if (nsProvider === undefined) {
            nsProvider = 'plugin';
        }
        // Store the nsProvider back onto the moduleSpec.
        moduleSpec.nsProvider = nsProvider;
    }

    var srcPath = undefined;
    if (srcType === 'js') {
        srcPath = moduleSpec.moduleName + '.js';
    } else if (srcType === 'css') {
        srcPath = moduleSpec.moduleName + '/style.css';
    } else {
        throw 'Unsupported srcType "'+ srcType + '".';
    }

    if (nsProvider === 'plugin') {
        return exports.getPluginJSModulesPath(moduleSpec.namespace) + '/' + srcPath;
    } if (nsProvider === 'core-assets') {
        return exports.getCoreAssetsJSModulesPath(moduleSpec.namespace) + '/' + srcPath;
    } else {
        throw 'Unsupported namespace provider: ' + nsProvider;
    }
};

exports.getPluginJSModulesPath = function(pluginId) {
    return exports.getPluginPath(pluginId) + '/jsmodules';
};

exports.getCoreAssetsJSModulesPath = function(namespace) {
    return getRootURL() + '/assets/' + namespace + '/jsmodules';
};

exports.getPluginPath = function(pluginId) {
    return getRootURL() + '/plugin/' + pluginId;
};

exports.getHeadElement = function() {
    var window = windowHandle.getWindow();
    var docHead = window.document.getElementsByTagName("head");
    if (!docHead || docHead.length == 0) {
        throw 'No head element found in document.';
    }
    return docHead[0];
};

exports.setRootURL = function(url) {    
    if (!jenkinsCIGlobal) {
        exports.initJenkinsGlobal();
    }
    jenkinsCIGlobal.rootURL = url;
};

exports.parseResourceQName = function(resourceQName) {
    var qNameTokens = resourceQName.split(":");
    if (qNameTokens.length === 2) {
        var namespace = qNameTokens[0].trim();
        var nsTokens = namespace.split("/");
        var namespaceProvider = undefined;
        if (nsTokens.length === 2) {
            namespaceProvider = nsTokens[0].trim();
            namespace = nsTokens[1].trim();
            if (namespaceProvider !== 'plugin' && namespaceProvider !== 'core-assets') {
                console.error('Unsupported module namespace provider "' + namespaceProvider + '". Setting to undefined.');
                namespaceProvider = undefined;
            }
        }
        return {
            nsProvider: namespaceProvider,
            namespace: namespace,
            moduleName: qNameTokens[1].trim()
        };
    } else {
        // The module/bundle is not in a namespace and doesn't
        // need to be loaded i.e. it will load itself and export.
        return {
            moduleName: qNameTokens[0].trim()
        };
    }
};

exports.getModule = function(moduleSpec) {
    if (moduleSpec.namespace) {
        var plugin = exports.getNamespace(moduleSpec.namespace);
        return plugin[moduleSpec.moduleName];
    } else {
        var globals = exports.getGlobalModules();
        return globals[moduleSpec.moduleName];
    }
};

exports.getModuleSpec = function(moduleQName) {
    var moduleSpec = exports.parseResourceQName(moduleQName);
    var moduleNamespaceObj = exports.getModuleNamespaceObj(moduleSpec);
    if (moduleNamespaceObj) {
        var loading = getLoadingModule(moduleNamespaceObj, moduleSpec.moduleName);
        if (loading && loading.moduleSpec) {
            return loading.moduleSpec;
        }
    }
    return moduleSpec;
};

function getScriptId(scriptSrc, config) {
    if (typeof config === 'string') {
        return config;
    } else if (typeof config === 'object' && config.scriptId) {
        return config.scriptId;
    } else {
        return 'jenkins-script:' + scriptSrc;
    }    
}

exports.getRootURL = getRootURL;
function getRootURL() {
    if (jenkinsCIGlobal && jenkinsCIGlobal.rootURL) {
        return jenkinsCIGlobal.rootURL;
    }
    
    var docHead = exports.getHeadElement();
    var resURL = getAttribute(docHead, "data-resurl");

    if (!resURL) {
        var resURL = getAttribute(docHead, "resURL");
    
        if (!resURL) {
            throw "Attribute 'data-resurl' not defined on the document <head> element.";
        }
    }

    if (jenkinsCIGlobal) {
        jenkinsCIGlobal.rootURL = resURL;
    }
    
    return resURL;
}

exports.getAdjunctURL = getAdjunctURL;
function getAdjunctURL() {
    if (jenkinsCIGlobal && jenkinsCIGlobal.adjunctURL) {
        return jenkinsCIGlobal.adjunctURL;
    }
    
    var docHead = exports.getHeadElement();
    var adjunctURL = getAttribute(docHead, "data-adjuncturl");

    if (!adjunctURL) {
        throw "Attribute 'data-adjuncturl' not defined on the document <head> element.";
    }

    if (jenkinsCIGlobal) {
        jenkinsCIGlobal.adjunctURL = adjunctURL;
    }
    
    return adjunctURL;
}

function createElement(name) {
    var document = windowHandle.getWindow().document;
    return document.createElement(name);
}

function getAttribute(element, attributeName) {
    var value = element.getAttribute(attributeName.toLowerCase());
    
    if (value) {
        return value;
    } else {
        // try without lowercasing
        return element.getAttribute(attributeName);
    }    
}

function getLoadingModule(moduleNamespaceObj, moduleName) {
    if (!moduleNamespaceObj.loadingModules) {
        moduleNamespaceObj.loadingModules = {};
    }
    if (!moduleNamespaceObj.loadingModules[moduleName]) {
        moduleNamespaceObj.loadingModules[moduleName] = {};
    }
    return moduleNamespaceObj.loadingModules[moduleName];
}

function endsWith(string, suffix) {
    return (string.indexOf(suffix, string.length - suffix.length) !== -1);
}

function thisBundleNamespaceProvider() {
    if (whoami !== undefined) {
        return whoami.nsProvider;
    }
    return undefined;
}

function getBundleNSProviderFromScriptElement(namespace, moduleName) {
    var docHead = exports.getHeadElement();
    var scripts = docHead.getElementsByTagName("script");

    for (var i = 0; i < scripts.length; i++) {
        var script = scripts[i];
        var elNamespace = script.getAttribute('data-jenkins-module-namespace');
        var elModuleName = script.getAttribute('data-jenkins-module-moduleName');

        if (elNamespace === namespace && elModuleName === moduleName) {
            return script.getAttribute('data-jenkins-module-nsProvider');
        }
    }

    return undefined;
}

},{"./promise":3,"window-handle":4}],3:[function(require,module,exports){
/*
 * Very simple "Promise" impl.
 * <p>
 * Intentionally not using the "promise" module/polyfill because it will add a few Kb and we 
 * only need something very simple here. We really just want to follow the main pattern
 * and don't need some of the fancy stuff.
 * <p>
 * I think so long as we stick to same interface/interaction pattern as outlined in the link
 * below, then we can always switch to the "promise" module later without breaking anything.
 * <p>
 * See https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Promise
 */

exports.make = function(executor) {
    var thePromise = new APromise();
    executor.call(thePromise, function(result) {
        thePromise.resolve(result);
    }, function(reason) {
        thePromise.reject(reason);
    });
    return thePromise;
};

function APromise() {
    this.state = 'PENDING';
    this.whenFulfilled = undefined;
    this.whenRejected = undefined;
    this.applyFulfillArgs = false;
}

APromise.prototype.applyArgsOnFulfill = function() {
    this.applyFulfillArgs = true;
    return this;
}

APromise.prototype.resolve = function (result) {
    this.state = 'FULFILLED';
    
    var thePromise = this;
    function doFulfill(whenFulfilled, result) {
        if (thePromise.applyFulfillArgs) {
            whenFulfilled.apply(whenFulfilled, result);
        } else {
            whenFulfilled(result);
        }
    }
    
    if (this.whenFulfilled) {
        doFulfill(this.whenFulfilled, result);
    }
    // redefine "onFulfilled" to call immediately
    this.onFulfilled = function (whenFulfilled) {
        if (whenFulfilled) {
            doFulfill(whenFulfilled, result);
        }
        return this;
    }
};

APromise.prototype.reject = function (reason) {
    this.state = 'REJECTED';
    if (this.whenRejected) {
        this.whenRejected(reason);
    }
    // redefine "onRejected" to call immediately
    this.onRejected = function(whenRejected) {
        if (whenRejected) {
            whenRejected(reason);
        }
        return this;
    }
};

APromise.prototype.onFulfilled = function(whenFulfilled) {
    if (!whenFulfilled) {
        throw 'Must provide an "whenFulfilled" callback.';
    }
    this.whenFulfilled = whenFulfilled;
    return this;
};

APromise.prototype.onRejected = function(whenRejected) {        
    if (whenRejected) {
        this.whenRejected = whenRejected;
    }
    return this;
};

},{}],4:[function(require,module,exports){
var theWindow;
var defaultTimeout = 10000;
var callbacks = [];
var windowSetTimeouts = [];

function execCallback(callback, theWindow) {
    if (callback) {
        try {
            callback.call(callback, theWindow);                
        } catch (e) {
            console.log("Error invoking window-handle callback.");
            console.log(e);
        }
    }
}

/**
 * Get the global "window" object.
 * @param callback An optional callback that can be used to receive the window asynchronously. Useful when
 * executing in test environment i.e. where the global window object might not exist immediately. 
 * @param timeout The timeout if waiting on the global window to be initialised.
 * @returns {*}
 */
exports.getWindow = function(callback, timeout) {
    
	if (theWindow) {
        execCallback(callback, theWindow);
        return theWindow;
	} 
	
	try {
		if (window) {
            execCallback(callback, window);
			return window;
		} 
	} catch (e) {
		// no window "yet". This should only ever be the case in a test env.
		// Fall through and use callbacks, if supplied.
	}

	if (callback) {
        function waitForWindow(callback) {
            callbacks.push(callback);
            var windowSetTimeout = setTimeout(function() {
                callback.error = "Timed out waiting on the window to be set.";
                callback.call(callback);
            }, (timeout?timeout:defaultTimeout));
            windowSetTimeouts.push(windowSetTimeout);
        }
        waitForWindow(callback);
	} else {
		throw "No 'window' available. Consider providing a 'callback' and receiving the 'window' async when available. Typically, this should only be the case in a test environment.";
	}
}

/**
 * Set the global window e.g. in a test environment.
 * <p>
 * Once called, all callbacks (registered by earlier 'getWindow' calls) will be invoked.
 * 
 * @param newWindow The window.
 */
exports.setWindow = function(newWindow) {
	for (var i = 0; i < windowSetTimeouts.length; i++) {
		clearTimeout(windowSetTimeouts[i]);
	}
    windowSetTimeouts = [];
	theWindow = newWindow;
	for (var i = 0; i < callbacks.length; i++) {
		execCallback(callbacks[i], theWindow);
	}
    callbacks = [];
}

/**
 * Set the default time to wait for the global window to be set.
 * <p>
 * Default is 10 seconds (10000 ms).
 * 
 * @param millis Milliseconds to wait for the global window to be set.
 */
exports.setDefaultTimeout = function(millis) {
    defaultTimeout = millis;
}
},{}],5:[function(require,module,exports){
require('jenkins-js-modules').whoami('undefined:add-item');

require('jenkins-js-modules')
    .import('core-assets/jquery-detached:jquery2')
    .onFulfilled(function() {

// Initialize all modules by requiring them. Also makes sure they get bundled (see gulpfile.js).
var $ = require('jenkins-js-modules').require('core-assets/jquery-detached:jquery2').getJQuery();

var getItems = function() {
  var d = $.Deferred();
  $.get('itemCategories?depth=3&iconStyle=icon-xlg').done(
    function(data){
      d.resolve(data);
    }
  );
  return d.promise();
}; 

var jRoot = $('head').attr('data-rooturl');

$.when(getItems()).done(function(data) {
  $(function() {

    //////////////////////////
    // helper functions...

    function parseResponseFromCheckJobName(data) {
      var html = $.parseHTML(data);
      var element = html[0];
      if (element !== undefined) {
        return $(element).text();
      }
      return undefined;
    }

    function cleanClassName(className){
      return className.replace(/\./g,'_');
    }

    function checkForLink(desc) {
      if (desc.indexOf('&lt;a href="') === -1) {
        return desc;
      }
      var newDesc = desc.replace(/\&lt;/g,'<').replace(/\&gt;/g,'>');
      return newDesc;
    }

    function getCopyFromValue() {
      return $('input[type="text"][name="from"]', '#createItem').val();
    }

    function isItemNameEmpty() {
      var itemName = $('input[name="name"]', '#createItem').val();
      return (itemName === '') ? true : false;
    }

    function getFieldValidationStatus(fieldId) {
      return $('#' + fieldId).data('valid');
    }

    function setFieldValidationStatus(fieldId, status) {
      $('#' + fieldId).data('valid', status);
    }

    function activateValidationMessage(messageId, context, message) {
      if (message !== undefined && message !== '') {
        $(messageId, context).html('&#187; ' + message);
      }
      cleanValidationMessages(context);
      hideInputHelp(context);
      $(messageId).removeClass('input-message-disabled');
    }

    function cleanValidationMessages(context) {
      $(context).find('.input-validation-message').addClass('input-message-disabled');
    }

    function hideInputHelp(context) {
      $('.input-help', context).addClass('input-message-disabled');
    }

    function showInputHelp(context) {
      $('.input-help', context).removeClass('input-message-disabled');
    }

    // About Scroll-linked effect: https://developer.mozilla.org/en-US/docs/Mozilla/Performance/Scroll-linked_effects
    function doSticky() {
      var decorator = $('form .footer .btn-decorator');
      var pos = decorator.offset();
      var vpH = $(window).height();
      if (pos.top >= vpH) {
        decorator.css({position: 'fixed'});
      }

      $(window).scroll(function() {
        var footer = $('form .footer');
        var ref1 = decorator.offset().top + decorator.outerHeight();
        var ref2 = footer.offset().top + footer.outerHeight();
        var vpH = $(window).height();
        if (ref2 > vpH + $(window).scrollTop()) {
          decorator.css({position: 'fixed'});
        } else if (ref2 - 1 <= ref1) {
          decorator.css({position: 'absolute'});
        }
      });
    }

    function enableSubmit(status) {
      var btn = $('form .footer .btn-decorator button[type=submit]');
      if (status === true) {
        if (btn.hasClass('disabled')) {
          btn.removeClass('disabled');
          btn.prop('disabled', false);
        }
      } else {
        if (!btn.hasClass('disabled')) {
          btn.addClass('disabled');
          btn.prop('disabled', true);
        }
      }
    }

    function getFormValidationStatus() {
      if (getFieldValidationStatus('name') && (getFieldValidationStatus('items') || getFieldValidationStatus('from'))) {
          return true;
      }
      return false;
    }

    function cleanItemSelection() {
      $('.categories').find('li[role="radio"]').attr("aria-checked", "false");
      $('#createItem').find('input[type="radio"][name="mode"]').removeAttr('checked');
      $('.categories').find('.active').removeClass('active');
      setFieldValidationStatus('items', false);
    }

    function cleanCopyFromOption() {
      $('#createItem').find('input[type="radio"][value="copy"]').removeAttr('checked');
      $('input[type="text"][name="from"]', '#createItem').val('');
      setFieldValidationStatus('from', false);
    }


    //////////////////////////////////
    // Draw functions

    function drawCategory(category) {
      var $category = $('<div/>').addClass('category').attr('id', 'j-add-item-type-' + cleanClassName(category.id));
      var $items = $('<ul/>').addClass('j-item-options');
      var $catHeader = $('<div class="header" />');
      var title = '<h2>' + category.name + '</h2>';
      var description = '<p>' + category.description + '</p>';

      // Add items
      $.each(category.items, function(i, elem) {
        $items.append(drawItem(elem));
      });

      $catHeader.append(title);
      $catHeader.append(description);
      $category.append($catHeader);
      $category.append($items);

      return $category;
    }

    function drawItem(elem) {
      var desc = checkForLink(elem.description);
      var $item = $(['<li tabindex="0" role="radio" aria-checked="false" class="', cleanClassName(elem.class), '"><label><input type="radio" name="mode" value="',
      elem.class ,'"/> <span class="label">', elem.displayName, '</span></label></li>'].join('')).append(['<div class="desc">', desc, '</div>'].join('')).append(drawIcon(elem));

      function select(e) {
        e.preventDefault();
        cleanCopyFromOption();
        cleanItemSelection();

        $(this).attr("aria-checked", "true");
        $(this).find('input[type="radio"][name="mode"]').prop('checked', true);
        $(this).addClass('active');

        setFieldValidationStatus('items', true);
        if (!getFieldValidationStatus('name')) {
          $('input[name="name"][type="text"]', '#createItem').focus();
        } else {
          if (getFormValidationStatus()) {
            enableSubmit(true);
          }
        }
      }

      $item.click(select);

      $item.keypress(function(e) {
        switch (e.which) {
          case 13:
          case 32:
            $(this).trigger('click');
            e.stopPropagation();
            break;
        }
      });

      return $item;
    }

    function drawIcon(elem) {
      var $icn;
      if (elem.iconClassName && elem.iconQualifiedUrl) {
        $icn = $('<div class="icon">');
        $(['<img class="', elem.iconClassName, ' icon-xlg" src="', elem.iconQualifiedUrl, '">'].join('')).appendTo($icn);
      } else if (elem.iconFilePathPattern) {
        $icn = $('<div class="icon">');
        var iconFilePath = jRoot + '/' + elem.iconFilePathPattern.replace(":size", "48x48");
        $(['<img src="', iconFilePath, '">'].join('')).appendTo($icn);
      } else {
        $icn = $('<div class="default-icon">');
        var colors = ['c-49728B','c-335061','c-D33833','c-6D6B6D', 'c-6699CC'];
        var desc = elem.description || '';
        var name = elem.displayName;
        var colorClass= colors[(desc.length) % 4];
        var aName = name.split(' ');
        var a = name.substring(0,1);
        var b = ((aName.length === 1) ? name.substring(1,2) : aName[1].substring(0,1));
        $(['<span class="a">',a,'</span><span class="b">',b,'</span>'].join('')).appendTo($icn);
        $icn.addClass(colorClass);
      }
      return $icn;
    }

    // The main panel content is hidden by default via an inline style. We're ready to remove that now.
    $('#add-item-panel').removeAttr('style');

    // Render all categories
    var $categories = $('div.categories');
    $.each(data.categories, function(i, elem) {
      drawCategory(elem).appendTo($categories);
    });

    // Focus
    $("#add-item-panel").find("#name").focus();

    // Init NameField
    $('input[name="name"]', '#createItem').on("blur input", function() {
      if (!isItemNameEmpty()) {
        var itemName = $('input[name="name"]', '#createItem').val();
        $.get("checkJobName", { value: itemName }).done(function(data) {
          var message = parseResponseFromCheckJobName(data);
          if (message !== '') {
            activateValidationMessage('#itemname-invalid', '.add-item-name', message);
          } else {
            cleanValidationMessages('.add-item-name');
            showInputHelp('.add-item-name');
            setFieldValidationStatus('name', true);
            if (getFormValidationStatus()) {
              enableSubmit(true);
            }
          }
        });
      } else {
        enableSubmit(false);
        setFieldValidationStatus('name', false);
        cleanValidationMessages('.add-item-name');
        activateValidationMessage('#itemname-required', '.add-item-name');
      }
    });

    // Init CopyFromField
    $('input[name="from"]', '#createItem').blur(function() {
      if (getCopyFromValue() === '') {
        $('#createItem').find('input[type="radio"][value="copy"]').removeAttr('checked');
      } else {
        cleanItemSelection();
        $('#createItem').find('input[type="radio"][value="copy"]').prop('checked', true);
        setFieldValidationStatus('from', true);
        if (!getFieldValidationStatus('name')) {
          activateValidationMessage('#itemname-required', '.add-item-name');
          setTimeout(function() {
            $('input[name="name"][type="text"]', '#createItem').focus();
          }, 400);
        } else {
          if (getFormValidationStatus()) {
            enableSubmit(true);
          }
        }
      }
    });

    // Client-side validation
    $("#createItem").submit(function(event) {
      if (!getFormValidationStatus()) {
        event.preventDefault();
        if (!getFieldValidationStatus('name')) {
          activateValidationMessage('#itemname-required', '.add-item-name');
          $('input[name="name"][type="text"]', '#createItem').focus();
        } else {
          if (!getFieldValidationStatus('items') && !getFieldValidationStatus('from'))  {
            activateValidationMessage('#itemtype-required', '.add-item-name');
            $('input[name="name"][type="text"]', '#createItem').focus();
          }
        }
      }
    });

    // Disable the submit button
    enableSubmit(false);

    // Do sticky the form buttons
    doSticky();
  });
});

		require('jenkins-js-modules').export(undefined, 'add-item', {});
    });


},{"jenkins-js-modules":1}]},{},[5]);
