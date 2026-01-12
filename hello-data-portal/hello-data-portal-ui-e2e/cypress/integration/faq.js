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
describe('FAQ', function () {
    it('Should enter faq management menu.', function () {
        cy.loginAsAdmin();
        cy.contains('Dashboards');
        cy.contains('HelloDATA');
        cy.wait(500);
        cy.get('#faqManagementMenu').should('exist');
        cy.get('#faqManagementMenu').click({ force: true });
        cy.url().should('include', '/faq-management');
        cy.contains('FAQ Verwaltung');
    });
    // it('Should create faq.', () => {
    //     cy.loginAsAdmin();
    //     cy.contains('Dashboards');
    //     cy.contains('HelloDATA');
    //     cy.wait(500);
    //     cy.get('#faqManagementMenu').should('exist');
    //     cy.get('#faqManagementMenu').click({force: true});
    //     cy.wait(500);
    //     cy.url().should('include', '/faq-management');
    //     cy.contains('FAQ Verwaltung');
    //
    //     // create faq
    //     cy.get('#createFaqButton').click();
    //     cy.wait(500);
    //     cy.url().should('include', '/faq-management/create');
    //     cy.get('#title').clear();
    //     cy.get('#title').type('Test FAQ');
    //     cy.get('#faqMessage').clear();
    //     cy.get('#faqMessage').type('Test FAQ');
    //     cy.get('#saveFaqButton').scrollIntoView().click();
    //     cy.wait(500);
    //     cy.url().should('include', '/faq-management');
    //
    // });
    it('Should create, update, delete faq.', function () {
        cy.loginAsAdmin();
        cy.contains('Dashboards');
        cy.contains('HelloDATA');
        cy.wait(500);
        cy.get('#faqManagementMenu').should('exist');
        cy.get('#faqManagementMenu').click({ force: true });
        cy.wait(500);
        cy.url().should('include', '/faq-management');
        cy.contains('FAQ Verwaltung');
        // create faq
        cy.get('#createFaqButton').click();
        cy.wait(500);
        cy.url().should('include', '/faq-management/create');
        cy.get('#title').clear();
        cy.get('#title').type('Test FAQ');
        cy.get('#faqMessage').clear();
        cy.get('#faqMessage').type('Test FAQ');
        cy.get('#saveFaqButton').scrollIntoView().click();
        cy.wait(500);
        cy.url().should('include', '/faq-management');
        // check if created exists on the list
        cy.contains('Test FAQ');
        // update faq
        cy.get('[id="editFaq_Test FAQ"]').should('exist');
        cy.get('[id="editFaq_Test FAQ"]').click({ force: true });
        cy.wait(500);
        cy.url().should('include', '/faq-management/edit');
        cy.get('#faqMessage').type('Test FAQ edited');
        cy.get('#saveFaqButton').scrollIntoView().click({ force: true });
        cy.wait(500);
        // check if is on the list
        cy.contains('Test FAQ');
        cy.url().should('include', '/faq-management');
        // delete faq
        cy.get('[id="deleteFaq_Test FAQ"]').should('exist');
        cy.get('[id="deleteFaq_Test FAQ"]').click({ force: true });
        cy.wait(500);
        cy.get('#deleteFaqButton').should('exist');
        cy.get('#deleteFaqButton').scrollIntoView().click({ force: true });
        cy.wait(500);
        cy.url().should('include', '/faq-management');
        cy.should('not.contain', 'Test FAQ');
    });
});
