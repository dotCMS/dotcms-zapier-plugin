/**
 * Contains Authentication tests 
*/

'use strict';

const chai = require('chai');
const expect = chai.expect;

const zapier = require('zapier-platform-core');
const nock = require('nock');

const App = require('../index');
const appTester = zapier.createAppTester(App);

describe('dotCMS Authentication', () => {
  zapier.tools.env.inject();

  const testURL = process.env.URL;

  // Resets the Nock interceptor after each test
  afterEach(() => {
      nock.cleanAll();
  });

  /**
   * Verifies the dotCMS basic api authentication succeeds
   * when all parameters are provided
  */
  it('should authenticate', (done) => {

    const inputData = {
      user: process.env.EMAIL,
      password: process.env.PASSWORD,
      expirationDays: 730,
      label: 'Zapier Integration'
    }

    // Mock the API response
    nock(testURL).post('/api/v1/authentication/api-token', inputData)
    .reply(200, {
        entity: {
          token: 'Test Token'
        },
        errors: [],
        i18nMessagesMap: {},
        messages: [],
        permissions: []
      }
    ); 

    // Prepare the Zapier payload
    const bundle = {
      authData: {
        url: process.env.URL,
        email: process.env.EMAIL,
        password: process.env.PASSWORD
      }
    };

    // Perform the test 
    appTester(App.authentication.sessionConfig.perform, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('apiKey');
      expect(response.apiKey).to.equal('Test Token');
      done();
    })
    .catch(done);
  });

  /**
   * Verifies the dotCMS basic api authentication fails
   * when all parameters are not provided
  */
  it('should not authenticate', (done) => {

    const inputData = {
      user: process.env.EMAIL,
      expirationDays: 730,
      label: 'Zapier Integration'
    }

    // Mock the API response
    nock(testURL).post('/api/v1/authentication/api-token', inputData)
    .reply(400, [
      {
        errorCode: 'password',
        fieldName: null,
        message: 'may not be null'
      }
      ]
    ); 

    // Prepare the Zapier payload
    const bundle = {
      authData: {
        url: process.env.URL,
        email: process.env.EMAIL,
        password: process.env.PASSWORD
      }
    };

    // Perform the test 
    appTester(App.authentication.sessionConfig.perform, bundle)
    .then((response) => {
      done(new Error('API Authentication succeeded. This should not happen, it should fail'));
    })
    .catch((error) => {
      console.log('API authentication failed');
      done();
    });
  });

  /**
   * Verifies the dotCMS basic api authentication fails
   * when incorrect credentials are not provided
  */
   it('unauthorized response', (done) => {

    const inputData = {
      user: process.env.EMAIL,
      password: process.env.PASSWORD,
      expirationDays: 730,
      label: 'Zapier Integration'
    }

    // Mock the API response
    nock(testURL).post('/api/v1/authentication/api-token', inputData)
    .reply(401, {
        entity: '',
        errors: [
            {
              errorCode: 'authentication-failed',
              fieldName: null,
              message: 'Authentication failed.  Please try again.'
            }
        ],
        i18nMessagesMap: {},
        messages: [],
        permissions: []
      }
    ); 

    // Prepare the Zapier payload
    const bundle = {
      authData: {
        url: process.env.URL,
        email: process.env.EMAIL,
        password: process.env.PASSWORD
      }
    };

    // Perform the test 
    appTester(App.authentication.sessionConfig.perform, bundle)
    .then((response) => {
      done(new Error('API Authentication succeeded. This should not happen, it should fail'));
    })
    .catch((error) => {
      console.log('API authentication failed');
      done();
    });
  });

});
