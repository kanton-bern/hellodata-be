///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

import {PdfPageGeometry} from "../../store/pdf-export/pdf-export.model";

/** How one PDF page grid is drawn on the canvas. */
export interface PdfCanvasMetrics {
  /** Screen pixels per millimetre of the PDF. */
  pxPerMm: number;
  width: number;
  height: number;
  colWidth: number;
  rowHeight: number;
  /** Screen pixels per CSS pixel of the PDF; scales the PDF's px/pt text styles onto the canvas. */
  textScale: number;
}

/** CSS pixels per millimetre (96 per inch), the unit openhtmltopdf lays the PDF's px and pt out in. */
export const CSS_PX_PER_MM = 96 / 25.4;

/** The largest drawing of the page grid that fits into the available area, keeping the PDF's aspect
 *  ratio; null while there is no room (e.g. before the canvas is laid out). */
export function fitCanvas(geometry: PdfPageGeometry, availableWidth: number, availableHeight: number): PdfCanvasMetrics | null {
  const gridHeightMm = geometry.gridRowsPerPage * geometry.cellHeightMm;
  if (availableWidth <= 0 || availableHeight <= 0 || geometry.contentWidthMm <= 0 || gridHeightMm <= 0) {
    return null;
  }
  const pxPerMm = Math.min(availableWidth / geometry.contentWidthMm, availableHeight / gridHeightMm);
  const colWidth = (geometry.contentWidthMm / geometry.gridCols) * pxPerMm;
  const rowHeight = geometry.cellHeightMm * pxPerMm;
  return {
    pxPerMm,
    width: colWidth * geometry.gridCols,
    height: rowHeight * geometry.gridRowsPerPage,
    colWidth,
    rowHeight,
    textScale: pxPerMm / CSS_PX_PER_MM,
  };
}

/** Canvas height of a chart's image frame spanning `rows` grid rows, as the PDF sizes it. */
export function chartImageHeight(geometry: PdfPageGeometry, metrics: PdfCanvasMetrics, rows: number): number {
  return Math.max(1, rows * geometry.cellHeightMm - geometry.chartOverheadMm) * metrics.pxPerMm;
}

/** True when the PDF would cut some of `content` off: it reaches below the bottom of the `box` that
 *  clips it, or a word too long for its width sticks out to the side. Sub-pixel rounding is ignored. */
export function isCutOff(box: Element, content: Element): boolean {
  return content.getBoundingClientRect().bottom > box.getBoundingClientRect().bottom + 0.5
    || content.scrollWidth > content.clientWidth + 1;
}

const BLOCK_TAGS = new Set(['P', 'DIV', 'LI', 'UL', 'OL', 'H1', 'H2', 'H3', 'BLOCKQUOTE', 'PRE']);

/**
 * Collapses the white space of rendered text HTML the way the PDF renderer does: runs of spaces, tabs
 * and newlines become one space, and none is left at the start or end of a block or around a line
 * break. The canvas then lays the text out with `white-space: break-spaces` (see the SCSS) - a space
 * before a line break takes room there, as in openhtmltopdf - which would otherwise keep that white
 * space. `pre` blocks are left alone.
 */
export function collapseWhitespace(html: string): string {
  const template = document.createElement('template');
  template.innerHTML = html;
  let last: Text | null = null;          // the last text kept, to trim a trailing space at a break
  let afterSpace = true;                 // at a line start or after a space: a leading space goes
  const lineBreak = () => {
    if (last?.data.endsWith(' ')) {
      last.data = last.data.slice(0, -1);
      if (!last.data) {
        last.remove();
      }
    }
    last = null;
    afterSpace = true;
  };
  const walk = (parent: Node) => {
    for (const node of Array.from(parent.childNodes)) {
      if (node instanceof Text) {
        let text = node.data.replace(/[ \t\n\r\f]+/g, ' ');
        if (afterSpace) {
          text = text.replace(/^ /, '');
        }
        if (text) {
          node.data = text;
          last = node;
          afterSpace = text.endsWith(' ');
        } else {
          node.remove();
        }
      } else if (node instanceof HTMLElement) {
        const block = BLOCK_TAGS.has(node.tagName);
        if (block || node.tagName === 'BR') {
          lineBreak();
        }
        if (node.tagName !== 'PRE') {
          walk(node);
        }
        if (block) {
          lineBreak();
        }
      }
    }
  };
  walk(template.content);
  lineBreak();
  return template.innerHTML;
}
