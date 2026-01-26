
import { JSDOM } from 'jsdom';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Project root relative to this test file
const projectRoot = path.resolve(__dirname, '../../../../../../');
const assetsRoot = path.resolve(__dirname, '..');

console.log("Setting up JSDOM environment...");

const dom = new JSDOM(`<!DOCTYPE html>
<html>
<body>
  <div id="viewerContainer">
    <div id="viewer" class="pdfViewer"></div>
  </div>
</body>
</html>`, {
  url: "http://localhost/",
  runScripts: "dangerously",
  resources: "usable",
  pretendToBeVisual: true
});

function patchGlobal(key, value) {
  if (global[key] === undefined) {
    global[key] = value;
  } else {
    try {
        global[key] = value;
    } catch (e) {
        // console.log(`Cannot patch ${key}: ${e.message}`);
    }
  }
}

patchGlobal('window', dom.window);
patchGlobal('document', dom.window.document);
patchGlobal('HTMLElement', dom.window.HTMLElement);

if (!global.navigator) {
    try {
        Object.defineProperty(global, 'navigator', {
            value: dom.window.navigator,
            writable: true
        });
    } catch(e) {}
}

global.atob = (str) => Buffer.from(str, 'base64').toString('binary');
patchGlobal('Event', dom.window.Event);

// Polyfill requestAnimationFrame for JSDOM/Node
if (!global.requestAnimationFrame) {
    global.requestAnimationFrame = (callback) => setTimeout(callback, 0);
}
// Ensure window.requestAnimationFrame is also set
if (global.window && !global.window.requestAnimationFrame) {
    global.window.requestAnimationFrame = global.requestAnimationFrame;
}

// Mock DOMMatrix if missing
if (typeof DOMMatrix === 'undefined') {
    global.DOMMatrix = class DOMMatrix {
        constructor() {
            this.a = 1; this.b = 0; this.c = 0; this.d = 1; this.e = 0; this.f = 0;
        }
        scaleSelf() { return this; }
        translateSelf() { return this; }
        multiplySelf() { return this; }
    };
    if (global.window) global.window.DOMMatrix = global.DOMMatrix;
}

// Mock ImageData
if (typeof ImageData === 'undefined') {
    global.ImageData = class ImageData {
        constructor(width, height) {
            this.width = width;
            this.height = height;
            this.data = new Uint8ClampedArray(width * height * 4);
        }
    };
    if (global.window) global.window.ImageData = global.ImageData;
}

// Mock Path2D
if (typeof Path2D === 'undefined') {
    global.Path2D = class Path2D {};
    if (global.window) global.window.Path2D = global.Path2D;
}

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
};
if (global.window) global.window.ResizeObserver = global.ResizeObserver;

// Mock PdfAndroidJavascriptBridge
global.PdfAndroidJavascriptBridge = {
  getSize: () => 1024 * 1024,
  getChunk: (begin, end) => Buffer.alloc(end - begin).toString('base64'),
  onLoad: () => console.log('Mock: onLoad called'),
  onFailure: () => console.error('Mock: onFailure called')
};

// Read script.js
const scriptPath = path.join(assetsRoot, 'script.js');
let scriptContent = fs.readFileSync(scriptPath, 'utf8');

// We need to fix imports because we are running in Node, but the script expects relative paths from the HTML location
// The script uses './vendor/...' which assumes it's in 'app/src/main/assets/pdf/'
// We resolve these to absolute paths on disk for the test runner.

const vendorPath = path.join(assetsRoot, 'vendor');
const vendorUrl = 'file://' + vendorPath;

scriptContent = scriptContent.replace(
  /\.\/vendor\/pdf\.js\/5\.4\.530\/build\/pdf\.mjs/g,
  vendorUrl + '/pdf.js/5.4.530/build/pdf.mjs'
);
scriptContent = scriptContent.replace(
  /\.\/vendor\/pdf\.js\/5\.4\.530\/web\/pdf_viewer\.mjs/g,
  vendorUrl + '/pdf.js/5.4.530/web/pdf_viewer.mjs'
);
scriptContent = scriptContent.replace(
  /\.\/vendor\/pdf\.js\/5\.4\.530\/build\/pdf\.worker\.mjs/g,
  vendorUrl + '/pdf.js/5.4.530/build/pdf.worker.mjs'
);

// Mock canvas
import('canvas').then((canvas) => {
    patchGlobal('HTMLCanvasElement', dom.window.HTMLCanvasElement);
    runTest();
}).catch(err => {
    console.warn("Canvas not installed, running without canvas support (rendering might fail)");
    runTest();
});

function runTest() {
    // Write a temporary file for execution
    const tempTestFile = path.join(__dirname, 'temp_run_script.mjs');
    fs.writeFileSync(tempTestFile, scriptContent);

    console.log("Running script logic...");
    import(tempTestFile).then(() => {
        console.log("Script imported successfully.");
        // Wait for potential async operations
        setTimeout(() => {
            console.log("Async wait complete. If no errors above, test likely passed.");
            // Clean up
            fs.unlinkSync(tempTestFile);
        }, 2000);
    }).catch(error => {
        console.error("Script execution failed:", error);
        fs.unlinkSync(tempTestFile);
        process.exit(1);
    });
}
