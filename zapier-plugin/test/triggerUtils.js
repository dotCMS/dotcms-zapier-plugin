/**
 * Contains Zapier trigger utility operation tests 
*/

'use strict';

const chai = require('chai');
const expect = chai.expect;

const zapier = require('zapier-platform-core');
const nock = require('nock');

const utils = require('../utils');

const App = require('../index');
const appTester = zapier.createAppTester(App);

const testApiToken = 'Test Token';

describe('list content from dotcms', () => {
  zapier.tools.env.inject();

  const testURL = process.env.URL + '/api/v1/dotzapier';

  // Resets the Nock interceptor after each test
  afterEach(() => {
    nock.cleanAll();
  });
  
  /**
   * Verifies the content from dotCMS is sent to Zapier
  */
  it('should get content from dotcms', (done) => {
    // Mock the API response
    nock(testURL).get('/perform-list')
    .reply(200, {
        data: JSON.stringify([utils.sampleObject])
      }
    );  
    
    // Prepare the Zapier payload
    const bundle = {
      authData: {
        url: process.env.URL,
        apiKey: testApiToken
      },
      inputData: {} 
    };

  // Perform the test  
  appTester(App.triggers.publish.operation.performList, bundle)
  .then((response) => {
    expect(response).to.be.an('array');
    expect(response).to.have.lengthOf(1);

    const data = response[0];
    expect(data).to.have.property('id');

    done();
  })
  .catch(done);
  });

  /**
   * Verifies that the trigger url is sent to dotCMS
  */
  it('subscribe to a trigger from zapier', (done) => {
      
    const triggerName = 'publish';

    const inputData = {
      triggerName: triggerName
    };

    // Mock the API response
    nock(testURL).post('/subscribe', inputData)
    .reply(200, {
        id: triggerName
      }
    ); 

    // Prepare the Zapier payload
    const bundle = {
      authData: {
        url: process.env.URL,
        apiKey: testApiToken
      },
      inputData: inputData 
    };

    // Perform the test
    appTester(App.triggers.publish.operation.performSubscribe, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('id');
      expect(response.id).to.equal(triggerName);
      done();
    })
    .catch(done);
  });

  /**
   * Verifies that Zapier notifies dotCMS trigger has been disabled
  */
  it('unsubscribe to a trigger from zapier', (done) => {
      
    const triggerName = 'publish';

    // Mock the API response
    nock(testURL).delete('/unsubscribe' + '?triggerName=' + triggerName)
    .reply(200, {
        message: 'Zapier hook removed'
      }
    ); 

    // Prepare the Zapier payload
    const bundle = {
      authData: {
        url: process.env.URL,
        apiKey: testApiToken
      },
      subscribeData: {
        id: triggerName
      } 
    };

    // Perform the test
    appTester(App.triggers.publish.operation.performUnsubscribe, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('message');
      expect(response.message).to.equal('Zapier hook removed');
      done();
    })
    .catch(done);
  });

});