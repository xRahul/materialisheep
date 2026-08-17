// Modern pdf.js (pdfjs-dist 4.10.38, legacy build) minimal viewer.
// It expects PdfAndroidJavascriptBridge to be injected from the Android side
// (getSize/getChunk/onLoad/onFailure), keeping the same contract as the
// previous 1.9.658-based viewer.
import * as pdfjsLib from './vendor/pdfjs/build/pdf.mjs';

pdfjsLib.GlobalWorkerOptions.workerSrc = './vendor/pdfjs/build/pdf.worker.mjs';

const SCALE = 2;
const RENDER_BUFFER = 100; // px of pre/post-rendering around the viewport
const container = document.getElementById('viewerContainer');

let pdfDoc = null;
const pageEntries = new Map(); // pageNumber -> { page, div, canvas, viewport, rendered }
let scrollScheduled = false;

// Defines the interface pdf.js uses to fetch chunks of the PDF file from the
// Android bridge instead of the network.
class RangeTransport extends pdfjsLib.PDFDataRangeTransport {
  requestDataRange(begin, end) {
    const base64 = PdfAndroidJavascriptBridge.getChunk(begin, end);
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; ++i) {
      bytes[i] = binary.charCodeAt(i) & 0xff;
    }
    // Has to be async, otherwise pdf.js throws.
    setTimeout(() => {
      this.onDataRange(begin, bytes);
    }, 0);
  }
}

async function layoutPage(pageNumber) {
  const page = await pdfDoc.getPage(pageNumber);
  const viewport = page.getViewport({ scale: SCALE });

  // Placeholder div sized to the final page dimensions. The canvas is only
  // created when the page is actually rendered, so the backing stores of
  // off-screen pages never allocate (same behavior as the old viewer).
  const div = document.createElement('div');
  div.className = 'pdfPage';
  div.style.width = viewport.width + 'px';
  div.style.height = viewport.height + 'px';
  container.appendChild(div);

  pageEntries.set(pageNumber, { page, div, viewport, rendered: false });
}

async function renderPage(pageNumber) {
  let entry = pageEntries.get(pageNumber);
  if (!entry || entry.rendered) {
    return;
  }
  if (!entry.canvas) {
    const canvas = document.createElement('canvas');
    canvas.width = entry.viewport.width;
    canvas.height = entry.viewport.height;
    canvas.style.width = entry.viewport.width + 'px';
    canvas.style.height = entry.viewport.height + 'px';
    entry.canvas = canvas;
    entry.div.appendChild(canvas);
  }
  try {
    await entry.page.render({
      canvasContext: entry.canvas.getContext('2d'),
      viewport: entry.viewport,
    });
    entry.rendered = true;
  } catch (e) {
    console.error('Page render failed: ' + pageNumber, e);
    // Leave rendered=false so a later scroll pass retries.
  }
}

// The container height is unconstrained (WebFragment is not always fullscreen),
// so the document scroll position, not the container's, decides visibility.
function renderVisiblePages() {
  const top = window.scrollY - RENDER_BUFFER;
  const bottom = window.scrollY + window.innerHeight + RENDER_BUFFER;

  for (let n = 1; n <= pdfDoc.numPages; ++n) {
    const entry = pageEntries.get(n);
    if (!entry) {
      continue; // layout pass not finished yet
    }
    const pageTop = entry.div.offsetTop;
    const pageBottom = pageTop + entry.div.offsetHeight;
    if (pageBottom >= top && pageTop <= bottom) {
      renderPage(n);
    }
  }
}

function onScroll() {
  if (scrollScheduled) {
    return;
  }
  scrollScheduled = true;
  window.requestAnimationFrame(() => {
    scrollScheduled = false;
    renderVisiblePages();
  });
}

async function initializePdfViewer() {
  const fileSize = PdfAndroidJavascriptBridge.getSize();

  try {
    pdfDoc = await pdfjsLib.getDocument({
      length: fileSize,
      range: new RangeTransport(fileSize),
      disableAutoFetch: true,
      rangeChunkSize: 262144,
      cMapUrl: './vendor/pdfjs/cmaps/',
      cMapPacked: true,
      standardFontDataUrl: './vendor/pdfjs/standard_fonts/',
      canvasMaxAreaInBytes: 8388608, // 2 MP * 4 bytes, same as the old maxCanvasPixels
    }).promise;

    // Lay out every page as a placeholder div, then render only what is
    // inside (or near) the viewport.
    for (let n = 1; n <= pdfDoc.numPages; ++n) {
      await layoutPage(n);
    }
    renderVisiblePages();
    window.addEventListener('scroll', onScroll, { passive: true });

    PdfAndroidJavascriptBridge.onLoad();
  } catch (e) {
    console.error(e);
    PdfAndroidJavascriptBridge.onFailure();
  }
}

initializePdfViewer();
