/**
 * Starting point of the Zapier plugin
 * All the trigger and create operation needs to be defined in this file 
*/

/**
 * Import all the triggers and create Zapier operation
*/
const dotEventTrigger = require('./triggers/dotevent');

const cmdOperation = require('./creates/cmdOperation');

const authentication = require('./authentication');

/**
 * HTTP Middleware which will append the necessary headers.
 * It will be invoked for every HTTP request made by Zapier
 * except for authention api 
 * @param request Node HTTP Request object
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 */
 const includeApiKeyHeader = (request, z, bundle) => {
  request.headers = request.headers || {};
  if (bundle.authData.apiKey) {
      request.headers['authorization'] = 'Bearer ' + bundle.authData.apiKey;
  }
  request.headers['content-type'] = 'application/json';
  request.headers['accept'] = 'application/json';

  return request;
};

/**
 * HTTP Middleware to handle all the incoming responses to Zapier
 * If the status code is above 400, then a user-friendly error message 
 * is displayed to the user on the Zapier UI screen at the time of 
 * Zap creation 
*/
const handleHTTPError = (response, z) => {
  if (response.status >= 400) {
    const defaultErrorMessage = "Unable to process the request. Please try again later.";

    const data = response.json;
    const message = data.message;

    const errorMessage = message || defaultErrorMessage;

    throw new z.errors.Error(errorMessage, response.status.toString());
  }
  return response;
};

/**
 * Zapier App Object
*/
const App = {
  version: require('./package.json').version,
  platformVersion: require('zapier-platform-core').version,
  authentication: authentication.authentication,

  beforeRequest: [
    includeApiKeyHeader
  ],

  afterResponse: [
    handleHTTPError
  ],

  triggers: {
    [dotEventTrigger.key]: dotEventTrigger
  },

  creates: {
    [cmdOperation.key]: cmdOperation
  },

  resources: {
  },

  searches: {
  },
};

module.exports = App;
