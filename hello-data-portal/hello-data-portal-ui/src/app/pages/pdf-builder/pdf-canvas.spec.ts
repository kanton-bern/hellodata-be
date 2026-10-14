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

import {describe, expect, it} from '@jest/globals';
import {CSS_PX_PER_MM, chartImageHeight, collapseWhitespace, fitCanvas, isCutOff} from './pdf-canvas';
import {PdfPageGeometry} from '../../store/pdf-export/pdf-export.model';

// The page grids the backend serves (ReportTemplate, pinned by PdfTemplateDtoTest).
const PORTRAIT: PdfPageGeometry = {contentWidthMm: 178, cellHeightMm: 64, gridCols: 4, gridRowsPerPage: 4, chartOverheadMm: 15};
const LANDSCAPE: PdfPageGeometry = {contentWidthMm: 265, cellHeightMm: 42, gridCols: 4, gridRowsPerPage: 4, chartOverheadMm: 15};

describe('fitCanvas', () => {
  it('draws a portrait page tall and narrow, limited by the available height', () => {
    const canvas = fitCanvas(PORTRAIT, 1200, 512)!;
    expect(canvas.pxPerMm).toBeCloseTo(2);                    // 512 px / 256 mm
    expect(canvas.width).toBeCloseTo(356);
    expect(canvas.height).toBeCloseTo(512);
    expect(canvas.colWidth).toBeCloseTo(89);                  // 44.5 mm
    expect(canvas.rowHeight).toBeCloseTo(128);                // 64 mm
    expect(canvas.rowHeight).toBeGreaterThan(canvas.colWidth);
  });

  it('draws a landscape page wide and flat, limited by the available width', () => {
    const canvas = fitCanvas(LANDSCAPE, 530, 1000)!;
    expect(canvas.pxPerMm).toBeCloseTo(2);                    // 530 px / 265 mm
    expect(canvas.width).toBeCloseTo(530);
    expect(canvas.height).toBeCloseTo(336);                   // 168 mm
    expect(canvas.colWidth).toBeCloseTo(132.5);               // 66.25 mm
    expect(canvas.rowHeight).toBeCloseTo(84);                 // 42 mm
    expect(canvas.colWidth).toBeGreaterThan(canvas.rowHeight);
  });

  it('keeps the page aspect ratio whatever the available area', () => {
    for (const [w, h] of [[300, 900], [1600, 400], [800, 800]]) {
      const canvas = fitCanvas(PORTRAIT, w, h)!;
      expect(canvas.width / canvas.height).toBeCloseTo(178 / 256);
      expect(canvas.width).toBeLessThanOrEqual(w + 1e-9);
      expect(canvas.height).toBeLessThanOrEqual(h + 1e-9);
    }
  });

  it('scales the PDF text by the same factor as the page', () => {
    const canvas = fitCanvas(PORTRAIT, 1200, 512)!;
    expect(canvas.textScale).toBeCloseTo(2 / CSS_PX_PER_MM);  // 2 px/mm on screen vs 3.78 CSS px/mm in the PDF
    expect(fitCanvas(PORTRAIT, 10000, 256 * CSS_PX_PER_MM)!.textScale).toBeCloseTo(1);
  });

  it('has no canvas while there is no room', () => {
    expect(fitCanvas(PORTRAIT, 0, 500)).toBeNull();
    expect(fitCanvas(PORTRAIT, 500, 0)).toBeNull();
  });
});

describe('chartImageHeight', () => {
  it('leaves the chart heading overhead out of the spanned rows, like the PDF', () => {
    const canvas = fitCanvas(PORTRAIT, 1200, 512)!;
    expect(chartImageHeight(PORTRAIT, canvas, 2)).toBeCloseTo((2 * 64 - 15) * 2);
    expect(chartImageHeight(PORTRAIT, canvas, 1)).toBeCloseTo((64 - 15) * 2);
  });
});

describe('isCutOff', () => {
  const box = (top: number, bottom: number, scrollWidth = 100, clientWidth = 100) =>
    ({getBoundingClientRect: () => ({top, bottom}), scrollWidth, clientWidth}) as unknown as Element;

  it('is false when the content ends inside the box', () => {
    expect(isCutOff(box(0, 100), box(4, 100))).toBe(false);
    expect(isCutOff(box(0, 100), box(4, 60))).toBe(false);
  });

  it('ignores sub-pixel rounding at the edge', () => {
    expect(isCutOff(box(0, 100), box(4, 100.4))).toBe(false);
    expect(isCutOff(box(0, 100), box(4, 60, 100.6, 100))).toBe(false);
  });

  it('is true when the content reaches below the box', () => {
    expect(isCutOff(box(0, 100), box(4, 101))).toBe(true);
  });

  it('is true when a long word sticks out to the side', () => {
    expect(isCutOff(box(0, 100), box(4, 60, 140, 100))).toBe(true);
  });
});

describe('collapseWhitespace', () => {
  it('collapses runs of white space into one space', () => {
    expect(collapseWhitespace('<p>Die  Stimm-\nbeteiligung\t lag</p>')).toBe('<p>Die Stimm- beteiligung lag</p>');
  });

  it('drops white space between blocks and at the start and end of a block', () => {
    expect(collapseWhitespace('<h1> Titel </h1>\n<p>\n  Text  </p>\n<ul>\n  <li> eins </li>\n</ul>\n'))
      .toBe('<h1>Titel</h1><p>Text</p><ul><li>eins</li></ul>');
  });

  it('collapses spaces across inline elements', () => {
    expect(collapseWhitespace('<p>in den <strong> städtischen </strong> Gemeinden</p>'))
      .toBe('<p>in den <strong>städtischen </strong>Gemeinden</p>');
  });

  it('drops white space around line breaks', () => {
    expect(collapseWhitespace('<p>erste Zeile <br />\n zweite Zeile</p>')).toBe('<p>erste Zeile<br>zweite Zeile</p>');
  });

  it('keeps non-breaking spaces and preformatted text', () => {
    expect(collapseWhitespace('<p>47.3&nbsp;%</p><pre>a  b\n c</pre>')).toBe('<p>47.3&nbsp;%</p><pre>a  b\n c</pre>');
  });
});
