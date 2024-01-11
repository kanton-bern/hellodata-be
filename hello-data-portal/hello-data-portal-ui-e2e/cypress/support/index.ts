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

import jwtDecode, {JwtPayload} from "jwt-decode";

Cypress.Commands.add('dataTest', (value: string) => {
    return cy.get(`[data-test=${value}]`);
});

const backendUrl = Cypress.env('backendUrl') as string;
const baseUrl = Cypress.env('auth_base_url') as string;
const realm = Cypress.env('auth_realm') as string;
const clientId = Cypress.env('auth_client_id') as string;

Cypress.Commands.add('token',
    (usr: string, pwd: string) => {
        return cy.request(
            {
                url: `${baseUrl}/realms/${realm}/protocol/openid-connect/token`,
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
            },
        );
    }
);

Cypress.Commands.add('revokeToken',
    () => {
        let item = window.sessionStorage.getItem(`0-${clientId}`);
        let session = JSON.parse(item);
        let url = `${baseUrl}/realms/${realm}/protocol/openid-connect/logout`;
        if (session && session.authnResult && session.authnResult.id_token) {
            url += `?id_token_hint=${session.authnResult.id_token}`;
        }
        return cy.request(
            {
                url: url,
                method: 'GET'
            },
        );
    }
);

Cypress.Commands.add('login', (username: string, password: string) => {
    cy.token(username, password).then((response) => {
        const body = response.body;
        const decoded: JwtPayload = jwtDecode<JwtPayload>(body.id_token);
        const session = {
            authnResult: body,
            authzData: body.access_token,
            userData: decoded,
        };
        window.sessionStorage.setItem(`0-${clientId}`, JSON.stringify(session));
    });
});

Cypress.Commands.add('loginAsAdmin', () => {
    cy.visit('/');
    const username = Cypress.env('admin_username') as string;
    const password = Cypress.env('admin_password') as string;
    cy.login(username, password);
    cy.visit('/');
});

Cypress.Commands.add('signInViaBrowser',
    (username: string, password: string) => {
        cy.visit('');
        cy.wait(1000);
        cy.get('#loginBtn').focus().then(() => {
            cy.get('#loginBtn').click().then(() => {
                cy.wait(1000);
                cy.get('#username').clear();
                cy.get('#username').type(username);
                cy.get('#password').clear();
                cy.get('#password').type(password);
                cy.get('#kc-login').click();
            });
        });
    },
);

function clearSessionStorage() {
    window.sessionStorage.clear();
    window.localStorage.clear();
    cy.wait(1000);
}

Cypress.Commands.add('logout', () => {
    cy.revokeToken().then(() => clearSessionStorage());
});

