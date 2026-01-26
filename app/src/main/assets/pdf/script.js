import * as pdfjsLib from './vendor/pdf.js/5.4.530/build/pdf.mjs';

// Set global variable for pdf_viewer.mjs
globalThis.pdfjsLib = pdfjsLib;

pdfjsLib.GlobalWorkerOptions.workerSrc = './vendor/pdf.js/5.4.530/build/pdf.worker.mjs';

// It expects PdfAndroidJavascriptBridge to be injected from the Android side
(async function () {
  // Dynamic import to ensure globalThis.pdfjsLib is set before pdf_viewer.mjs runs
  const {
    PDFViewer,
    PDFLinkService,
    EventBus
  } = await import('./vendor/pdf.js/5.4.530/web/pdf_viewer.mjs');

  var pdfViewer;

  function initializePdfViewer() {
    var container = document.getElementById('viewerContainer');
    var eventBus = new EventBus();

    // enable hyperlinks within PDF files.
    var pdfLinkService = new PDFLinkService({
      eventBus: eventBus,
    });

    pdfViewer = new CustomPdfViewer({
      container: container,
      eventBus: eventBus,
      linkService: pdfLinkService,
      useOnlyCssZoom: true,
    });
    pdfLinkService.setViewer(pdfViewer);

    // set proper scale to fit page width
    eventBus.on("pagesinit", function (e) {
      pdfViewer.currentScaleValue = "page-width";
    });

    var fileSize = PdfAndroidJavascriptBridge.getSize();

    pdfjsLib.getDocument({
      length: fileSize,
      range: new RangeTransport(fileSize),
      rangeChunkSize: 262144,
      disableAutoFetch: true,
      disableStream: true,
    }).promise.then(function (pdfDocument) {
      pdfViewer.setDocument(pdfDocument);
      pdfLinkService.setDocument(pdfDocument, null);
      PdfAndroidJavascriptBridge.onLoad();
    }).catch(function (e) {
      console.error(e);
      PdfAndroidJavascriptBridge.onFailure();
    });
  }

  // Defines the interface, which PDF.JS uses to fetch chunks of data it needs
  // for rendering a PDF doc.
  class RangeTransport extends pdfjsLib.PDFDataRangeTransport {
    constructor(size) {
      super(size, []);
    }

    requestDataRange(begin, end) {
      var base64string = PdfAndroidJavascriptBridge.getChunk(begin, end);
      var binaryString = atob(base64string);
      var byteArray = stringToBytes(binaryString)
      // Has to be async, otherwise PDF.js will fire an exception
      setTimeout(() => {
        this.onDataRange(begin, byteArray);
      }, 0);
    }
  }

  function stringToBytes(str) {
    var length = str.length;
    var bytes = new Uint8Array(length);
    for (var i = 0; i < length; ++i) {
      bytes[i] = str.charCodeAt(i) & 0xFF;
    }
    return bytes;
  }

  // Built-in PdfViewer uses `container`'s height to figure out what pages to render
  // We can't limit container's height because of how `WebFragment` works in non-fullscreen mode,
  // so we have to subclass existing PDFViewer and provide different logic for figuring out
  // what pages are visible - using `window.innerHeight` instead of `element.clientHeight`.
  class CustomPdfViewer extends PDFViewer {
    constructor(options) {
      super(options);
      // We need to override the scroll watching logic
      // PDFViewer likely initializes this.scroll in its constructor
      // We'll replace it with our own watcher
      this.scroll = watchScroll(window, () => {
        this.update();
      });
    }

    _getVisiblePages() {
      return getVisibleElements(window, this._pages, true);
    }
  }

  // Adapted from original script.js
  function getVisibleElements(scrollEl, views, sortByVisibility = false) {
    var top = window.scrollY,
        bottom = top + window.innerHeight;
    var left = window.scrollX,
        right = left + window.innerWidth;

    function isElementBottomBelowViewTop(view) {
      var element = view.div;
      var elementBottom = element.offsetTop + element.clientTop + element.clientHeight;
      return elementBottom > top;
    }
    var visible = [],
        view = void 0,
        element = void 0;
    var currentHeight = void 0,
        viewHeight = void 0,
        hiddenHeight = void 0,
        percentHeight = void 0;
    var currentWidth = void 0,
        viewWidth = void 0;
    var firstVisibleElementInd = views.length === 0 ? 0 : binarySearchFirstItem(views, isElementBottomBelowViewTop);
    for (var i = firstVisibleElementInd, ii = views.length; i < ii; i++) {
      view = views[i];
      element = view.div;
      currentHeight = element.offsetTop + element.clientTop;
      viewHeight = element.clientHeight;
      if (currentHeight > bottom) {
        break;
      }
      currentWidth = element.offsetLeft + element.clientLeft;
      viewWidth = element.clientWidth;
      if (currentWidth + viewWidth < left || currentWidth > right) {
        continue;
      }
      hiddenHeight = Math.max(0, top - currentHeight) + Math.max(0, currentHeight + viewHeight - bottom);
      percentHeight = (viewHeight - hiddenHeight) * 100 / viewHeight | 0;
      visible.push({
        id: view.id,
        x: currentWidth,
        y: currentHeight,
        view: view,
        percent: percentHeight
      });
    }
    var first = visible[0];
    var last = visible[visible.length - 1];
    if (sortByVisibility) {
      visible.sort(function (a, b) {
        var pc = a.percent - b.percent;
        if (Math.abs(pc) > 0.001) {
          return -pc;
        }
        return a.id - b.id;
      });
    }
    return {
      first: first,
      last: last,
      views: visible
    };
  }

  function binarySearchFirstItem(items, condition) {
    var minIndex = 0;
    var maxIndex = items.length - 1;
    if (items.length === 0 || !condition(items[maxIndex])) {
      return items.length;
    }
    if (condition(items[minIndex])) {
      return minIndex;
    }
    while (minIndex < maxIndex) {
      var currentIndex = minIndex + maxIndex >> 1;
      var currentItem = items[currentIndex];
      if (condition(currentItem)) {
        maxIndex = currentIndex;
      } else {
        minIndex = currentIndex + 1;
      }
    }
    return minIndex;
  }

  function watchScroll(viewAreaElement, callback) {
    var debounceScroll = function debounceScroll(evt) {
      if (rAF) {
        return;
      }
      rAF = window.requestAnimationFrame(function viewAreaElementScrolled() {
        rAF = null;
        var currentY = viewAreaElement.scrollY !== undefined ? viewAreaElement.scrollY : viewAreaElement.scrollTop;
        var lastY = state.lastY;
        if (currentY !== lastY) {
          state.down = currentY > lastY;
        }
        state.lastY = currentY;
        callback(state);
      });
    };
    var state = {
      down: true,
      lastY: viewAreaElement.scrollY !== undefined ? viewAreaElement.scrollY : viewAreaElement.scrollTop,
      _eventHandler: debounceScroll
    };
    var rAF = null;
    viewAreaElement.addEventListener('scroll', debounceScroll, true);
    return state;
  }

  initializePdfViewer();
}());
