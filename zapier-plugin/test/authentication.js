/**
 * Contains Authentication tests 
*/

'use strict';

const should = require('should');

const zapier = require('zapier-platform-core');

const App = require('../index');
const appTester = zapier.createAppTester(App);

describe('dotCMS Authentication', () => {
  zapier.tools.env.inject();

  /**
   * Verifies the dotCMS basic api authentication succeeds
   * when all parameters are provided
  */
  it('should authenticate', (done) => {
    const bundle = {
      authData: {
        url: process.env.URL,
        email: process.env.EMAIL,
        password: process.env.PASSWORD
      }
    };

    appTester(App.authentication.sessionConfig.perform, bundle)
    .then((response) => {
      should.exist(response.apiKey);
      done();
    })
    .catch(done);
  });

  /**
   * Verifies the dotCMS basic api authentication fails
   * when all parameters are not provided
  */
  it('should not authenticate', (done) => {
    const bundle = {
      authData: {
        url: process.env.URL
      }
    };

    appTester(App.authentication.sessionConfig.perform, bundle)
    .then((response) => {
      done(new Error("API Authentication succeeded. This should not happen, it should fail"));
    })
    .catch((error) => {
      console.log("API authentication failed");
      done();
    });
  });

});
