/**
 * Contains Zapier create operation tests 
*/

'use strict';

const chai = require('chai');
const expect = chai.expect;

const zapier = require('zapier-platform-core');
const nock = require('nock');

const App = require('../index');
const appTester = zapier.createAppTester(App);

const testApiToken = 'Test Token';

describe('zapier command actions', () => {
  zapier.tools.env.inject();

  const testURL = process.env.URL + '/api/v1/dotzapier';

  // Resets the Nock interceptor after each test
  afterEach(() => {
    nock.cleanAll();
  });
  
  /**
   * Verifies the content is saved on dotCMS
   * when a request arrives from Zapier
  */
  it('save a content on dotcms', (done) => {

    const triggerName = 'save';
      
    const inputData = {
      contentType: 'My Blog',
      text: `#${triggerName} #title=\"Lorem Ipsum\" #author=\"John Doe\" #publishDate=\"Sep 30 2022\" Lorem Ipsum is simply dummy text of the printing and typesetting industry`
    }; 

    // Mock the API response
    nock(testURL).post('/action', inputData)
    .reply(200, {
        message: `${triggerName} process successfully executed`
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
    appTester(App.creates.cmdOperation.operation.perform, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('message');
      expect(response.message).to.equal(`${triggerName} process successfully executed`);
      done();
    })
    .catch(done);
  });

  /**
   * Verifies the content is published on dotCMS
   * when a request arrives from Zapier
  */
  it('publish a content on dotcms', (done) => {

    const triggerName = 'publish';
      
    const inputData = {
      contentType: 'My Blog',
      text: `#${triggerName} #id=123456`
    }; 

    // Mock the API response
    nock(testURL).post('/action', inputData)
    .reply(200, {
        message: `${triggerName} process successfully executed`
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
    appTester(App.creates.cmdOperation.operation.perform, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('message');
      expect(response.message).to.equal(`${triggerName} process successfully executed`);
      done();
    })
    .catch(done);
  });

  /**
   * Verifies the content is unpublished on dotCMS
   * when a request arrives from Zapier
  */
  it('unpublish a content on dotcms', (done) => {

    const triggerName = 'unpublish';
      
    const inputData = {
      contentType: 'My Blog',
      text: `#${triggerName} #id=123456`
    }; 

    // Mock the API response
    nock(testURL).post('/action', inputData)
    .reply(200, {
        message: `${triggerName} process successfully executed`
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
    appTester(App.creates.cmdOperation.operation.perform, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('message');
      expect(response.message).to.equal(`${triggerName} process successfully executed`);
      done();
    })
    .catch(done);
  });

  /**
   * Verifies the content is archived on dotCMS
   * when a request arrives from Zapier
  */
  it('archive a content on dotcms', (done) => {

    const triggerName = 'archive';
      
    const inputData = {
      contentType: 'My Blog',
      text: `#${triggerName} #id=123456`
    }; 

    // Mock the API response
    nock(testURL).post('/action', inputData)
    .reply(200, {
        message: `${triggerName} process successfully executed`
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
    appTester(App.creates.cmdOperation.operation.perform, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('message');
      expect(response.message).to.equal(`${triggerName} process successfully executed`);
      done();
    })
    .catch(done);
  });

  /**
   * Verifies the content is unarchived on dotCMS
   * when a request arrives from Zapier
  */
  it('unarchive a content on dotcms', (done) => {

    const triggerName = 'unarchive';
      
    const inputData = {
      contentType: 'My Blog',
      text: `#${triggerName} #id=123456`
    }; 

    // Mock the API response
    nock(testURL).post('/action', inputData)
    .reply(200, {
        message: `${triggerName} process successfully executed`
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
    appTester(App.creates.cmdOperation.operation.perform, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('message');
      expect(response.message).to.equal(`${triggerName} process successfully executed`);
      done();
    })
    .catch(done);
  });

  /**
   * Verifies the content is deleted on dotCMS
   * when a request arrives from Zapier
  */
  it('delete a content on dotcms', (done) => {

    const triggerName = 'delete';
      
    const inputData = {
      contentType: 'My Blog',
      text: `#${triggerName} #id=123456`
    }; 

    // Mock the API response
    nock(testURL).post('/action', inputData)
    .reply(200, {
        message: `${triggerName} process successfully executed`
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
    appTester(App.creates.cmdOperation.operation.perform, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('message');
      expect(response.message).to.equal(`${triggerName} process successfully executed`);
      done();
    })
    .catch(done);
  });

  /**
   * Verifies the content is destroyed on dotCMS
   * when a request arrives from Zapier
  */
  it('destroy a content on dotcms', (done) => {

    const triggerName = 'destroy';
      
    const inputData = {
      contentType: 'My Blog',
      text: `#${triggerName} #id=123456`
    }; 

    // Mock the API response
    nock(testURL).post('/action', inputData)
    .reply(200, {
        message: `${triggerName} process successfully executed`
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
    appTester(App.creates.cmdOperation.operation.perform, bundle)
    .then((response) => {
      expect(response).to.be.an('object');
      expect(response).to.have.property('message');
      expect(response.message).to.equal(`${triggerName} process successfully executed`);
      done();
    })
    .catch(done);
  });

});