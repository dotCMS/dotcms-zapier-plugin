/**
 * Starting point of the Zapier plugin
 * All the trigger and create operation needs to be defined in this file 
*/

/**
 * Import all the triggers and create Zapier operation
*/
const publishTrigger = require('./triggers/publish');

const cmdOperation = require('./creates/cmdOperation');

const authentication = require('./authentication');

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
    authentication.includeApiKeyHeader
  ],

  afterResponse: [
    handleHTTPError
  ],

  triggers: {
    [publishTrigger.key]: publishTrigger
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
