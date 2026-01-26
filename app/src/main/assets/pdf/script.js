import * as pdfjsLib from './vendor/pdf.js/5.4.530/pdf.mjs';
import * as pdfjsViewer from './vendor/pdf.js/5.4.530/pdf_viewer.mjs';

pdfjsLib.GlobalWorkerOptions.workerSrc = './vendor/pdf.js/5.4.530/pdf.worker.mjs';

class RangeTransport extends pdfjsLib.PDFDataRangeTransport {
  constructor(size) {
    super(size, []);
    this.length = size;
  }

  requestDataRange(begin, end) {
    var base64string = PdfAndroidJavascriptBridge.getChunk(begin, end);
    var binaryString = atob(base64string);
    var byteArray = stringToBytes(binaryString);
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

class CustomPdfViewer extends pdfjsViewer.PDFViewer {
  constructor(options) {
    super(options);
    this.scroll = watchScroll(document, this._scrollUpdate.bind(this));
  }

  _getVisiblePages() {
    return getVisibleElements(this.container, this._pages, true);
  }
}

// Mostly copy-pasted from PDF.js source with modification for window.innerHeight
function getVisibleElements(scrollEl, views, sortByVisibility = false) {
  var top = window.scrollY,
      bottom = top + window.innerHeight;
  var left = scrollEl.scrollLeft,
      right = left + scrollEl.clientWidth;

  function isElementBottomBelowViewTop(view) {
    var element = view.div;
    var elementBottom = element.offsetTop + element.clientTop + element.clientHeight;
    return elementBottom > top;
  }

  var visible = [],
      view,
      element;
  var currentHeight,
      viewHeight,
      hiddenHeight,
      percentHeight;
  var currentWidth,
      viewWidth;
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
      var currentY = viewAreaElement.scrollTop;
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
    lastY: viewAreaElement.scrollTop,
    _eventHandler: debounceScroll
  };
  var rAF = null;
  viewAreaElement.addEventListener('scroll', debounceScroll, true);
  return state;
}

function initializePdfViewer() {
  var container = document.getElementById('viewerContainer');
  var eventBus = new pdfjsViewer.EventBus();

  // enable hyperlinks within PDF files.
  var pdfLinkService = new pdfjsViewer.PDFLinkService({
    eventBus: eventBus,
  });

  var pdfViewer = new CustomPdfViewer({
    container: container,
    eventBus: eventBus,
    linkService: pdfLinkService,
    maxCanvasPixels: 2097152,
  });
  pdfLinkService.setViewer(pdfViewer);

  // set proper scale to fit page width
  eventBus.on("pagesinit", function (e) {
    pdfViewer.currentScaleValue = 2;
  });

  var fileSize = PdfAndroidJavascriptBridge.getSize();

  pdfjsLib.getDocument({
    length: fileSize,
    range: new RangeTransport(fileSize),
    rangeChunkSize: 262144,
    disableAutoFetch: true,
  }).promise.then(function (pdfDocument) {
    pdfViewer.setDocument(pdfDocument);
    pdfLinkService.setDocument(pdfDocument, null);
    PdfAndroidJavascriptBridge.onLoad();
  }).catch(function (e) {
    console.error(e);
    PdfAndroidJavascriptBridge.onFailure();
  });
}

initializePdfViewer();
