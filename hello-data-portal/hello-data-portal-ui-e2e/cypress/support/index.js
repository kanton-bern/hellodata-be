"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
var jwt_decode_1 = require("jwt-decode");
Cypress.Commands.add('dataTest', function (value) {
    return cy.get("[data-test=".concat(value, "]"));
});
var backendUrl = Cypress.env('backendUrl');
var baseUrl = Cypress.env('auth_base_url');
var realm = Cypress.env('auth_realm');
var clientId = Cypress.env('auth_client_id');
Cypress.Commands.add('token', function (usr, pwd) {
    return cy.request({
        url: "".concat(baseUrl, "/realms/").concat(realm, "/protocol/openid-connect/token"),
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: {
            client_id: clientId,
            username: usr,
            password: pwd,
            grant_type: 'password',
            scope: 'openid'
        },
    });
});
Cypress.Commands.add('revokeToken', function () {
    var item = window.sessionStorage.getItem("0-".concat(clientId));
    var session = JSON.parse(item);
    var url = "".concat(baseUrl, "/realms/").concat(realm, "/protocol/openid-connect/logout");
    if (session && session.authnResult && session.authnResult.id_token) {
        url += "?id_token_hint=".concat(session.authnResult.id_token);
    }
    return cy.request({
        url: url,
        method: 'GET'
    });
});
Cypress.Commands.add('login', function (username, password) {
    cy.token(username, password).then(function (response) {
        var body = response.body;
        var decoded = (0, jwt_decode_1.default)(body.id_token);
        var session = {
            authnResult: body,
            authzData: body.access_token,
            userData: decoded,
        };
        window.sessionStorage.setItem("0-".concat(clientId), JSON.stringify(session));
    });
});
Cypress.Commands.add('loginAsAdmin', function () {
    cy.visit('/');
    var username = Cypress.env('admin_username');
    var password = Cypress.env('admin_password');
    cy.login(username, password);
    cy.visit('/');
});
Cypress.Commands.add('signInViaBrowser', function (username, password) {
    cy.visit('');
    cy.wait(1000);
    cy.get('#loginBtn').focus().then(function () {
        cy.get('#loginBtn').click().then(function () {
            cy.wait(1000);
            cy.get('#username').clear();
            cy.get('#username').type(username);
            cy.get('#password').clear();
            cy.get('#password').type(password);
            cy.get('#kc-login').click();
        });
    });
});
function clearSessionStorage() {
    window.sessionStorage.clear();
    window.localStorage.clear();
    cy.wait(1000);
}
Cypress.Commands.add('logout', function () {
    cy.revokeToken().then(function () { return clearSessionStorage(); });
});
