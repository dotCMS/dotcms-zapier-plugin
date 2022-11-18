/**
 * Contains Zapier trigger operation tests 
*/

'use strict';

const chai = require('chai');
const expect = chai.expect;

const zapier = require('zapier-platform-core');
const nock = require('nock');

const App = require('../index');
const appTester = zapier.createAppTester(App);

const testApiToken = 'Test Token';

describe('zapier trigger operation', () => {
    zapier.tools.env.inject();

    const testURL = process.env.URL + '/api/v1/dotzapier';

    // Resets the Nock interceptor after each test
    afterEach(() => {
        nock.cleanAll();
    });


    /**
     * Verifies that a DotEvent notification is sent to dotCMS
     * from Zapier
    */
    it('DotEvent trigger from zapier', (done) => {
        
        const triggerName = 'DotEvent';

        const inputData = {
            triggerName: triggerName
        };

        const cleanedData = {
            message: 'Zapier hook added'
        };

        // Mock the API response
        nock(testURL).post('/subscribe', inputData)
        .reply(200, cleanedData); 

        // Prepare the Zapier payload
        const bundle = {
          authData: {
            url: process.env.URL,
            apiKey: testApiToken
          },
          inputData: inputData,
          cleanedRequest: cleanedData
        };

        // Perform the test
        appTester(App.triggers.publish.operation.perform, bundle)
        .then((response) => {
            expect(response).to.be.an('array');
            expect(response).to.have.lengthOf(1);

            const data = response[0];
            expect(data).to.have.property('message');
            expect(data.message).to.equal('Zapier hook added');
            done();
        })
        .catch(done);
    });



});
