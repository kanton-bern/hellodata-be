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
describe('Profile page', function () {
    it('Should see roles in profile', function () {
        cy.loginAsAdmin();
        cy.get('.mr-4 > .ng-star-inserted > .route-link > .pl-2').click();
        cy.get('.p-menu-list > :nth-child(1) > .p-menuitem-link').click();
        cy.get('.field.ng-star-inserted > .mb-2').should('be.visible');
    });
    it('Should be redirected to home page', function () {
        cy.loginAsAdmin();
        cy.wait(500);
        cy.get('#userMenu').click();
        cy.wait(500);
        cy.contains('Profile').click();
        cy.wait(500);
        cy.get('.field.ng-star-inserted > .mb-2').should('be.visible');
        cy.get('span.fa-house').click();
        cy.wait(500);
        cy.get('#dashboardsHeader').scrollIntoView().should('be.visible');
        cy.get('#lineageHeader').scrollIntoView().should('be.visible');
        cy.get('#faqHeader').scrollIntoView().should('be.visible');
        cy.get('#dataMartsHeader').scrollIntoView().should('be.visible');
        cy.get('#documentationHeader').scrollIntoView().should('be.visible');
        cy.get('#externalDashboardsHeader').scrollIntoView().should('be.visible');
    });
});
