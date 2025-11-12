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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {SubsystemIframeComponent} from './subsystem-iframe.component';
import {ElementRef, SimpleChanges} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {CommonModule} from '@angular/common';
import {afterEach, beforeEach, describe, expect, it, jest} from "@jest/globals";
import {AuthService} from "../../services";

describe('SubsystemIframeComponent', () => {
  let component: SubsystemIframeComponent;
  let fixture: ComponentFixture<SubsystemIframeComponent>;

  // Create a BehaviorSubject to simulate accessToken changes
  const accessTokenSubject = new BehaviorSubject<string>('mock-access-token');

  // Mock AuthService
  const mockAuthService = {
    accessToken: accessTokenSubject.asObservable(), // Use the BehaviorSubject as an Observable
  };

  const mockElementRef: ElementRef = {
    nativeElement: document.createElement('div'),
  };

  // Mock document.cookie
  const mockDocumentCookie = {
    cookie: '',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, SubsystemIframeComponent],
      providers: [
        {provide: ElementRef, useValue: mockElementRef},
        {provide: AuthService, useValue: mockAuthService},
        // Provide the mocked document.cookie
        {provide: 'Document', useValue: mockDocumentCookie},
      ],
    });

    fixture = TestBed.createComponent(SubsystemIframeComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('url', 'https://example.com');
    fixture.componentRef.setInput('accessTokenInQueryParam', false);
    fixture.componentRef.setInput('delay', 0);

    // Initialize component
    fixture.detectChanges();
  });

  it('should create the SubsystemIframeComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should update frameUrl and emit iframeSetup on ngOnInit', () => {
    const emitSpy = jest.spyOn(component.iframeSetup, 'emit');

    component.ngOnInit();
    const changes: SimpleChanges = {
      url: {
        currentValue: 'https://example.com',
        previousValue: 'https://example.com',
        firstChange: false,
        isFirstChange: () => false,
      },
    };

    component.ngOnChanges(changes);

    fixture.detectChanges();

    expect(component.frameUrl).toBe('https://example.com');
  });

  it('should update frameUrl and emit iframeSetup on ngOnChanges', () => {
    const emitSpy = jest.spyOn(component.iframeSetup, 'emit');

    const changes: SimpleChanges = {
      url: {
        currentValue: 'https://new-url.com',
        previousValue: 'https://example.com',
        firstChange: false,
        isFirstChange: () => false,
      },
    };

    component.ngOnChanges(changes);
    fixture.detectChanges();

    expect(emitSpy).toHaveBeenCalledWith(true);
    expect(component.frameUrl).toBe('https://example.com');
  });

  it('should unsubscribe from accessTokenSub on ngOnDestroy', () => {
    const unsubscribeSpy = jest.spyOn(component.accessTokenSub, 'unsubscribe');

    component.ngOnDestroy();

    expect(unsubscribeSpy).toHaveBeenCalled();
  });

  afterEach(() => {
    // Clean up the subscription
    fixture.destroy();
  });
});
