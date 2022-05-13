/// <reference types="cypress" />

// Welcome to Cypress!
//
// This spec file contains a variety of sample tests
// for a todo list app that are designed to demonstrate
// the power of writing tests in Cypress.
//
// To learn more about how Cypress works and
// what makes it such an awesome testing tool,
// please read our getting started guide:
// https://on.cypress.io/introduction-to-cypress

describe('starwhale console', () => {
    beforeEach(() => {
        cy.visit('http://localhost:3000')
    })

    it('displays two todo items by default', () => {
        cy.get('[class^=Form_formItem]').eq(0).find('input').type('starwhale')
        cy.get('[class^=Form_formItem]').eq(1).find('input').type('abcd1234')

        cy.intercept('GET', '**/api/v1/user/current').as('getUser')

        cy.get('button').contains('login').click()

        cy.wait('@getUser').its('response.statusCode').should('eq', 200)

        cy.get('[class^=userNameWrapper]').should('have.text', 'starwhale')

        cy.get('table tr:nth-child(1) a').click()

        cy.get('nav[data-baseweb="side-navigation"] li').find('a[href*=jobs]').click()

        cy.get('[class^=userNameWrapper]').should('have.text', 'starwhale')

        cy.get('[class^=Card_cardHeadTitle]').should('have.text', 'Jobs')

        cy.get('table tr:nth-child(1) a').eq(0).click()
    })
})
