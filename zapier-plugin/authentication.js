/**
 * Contains methods for handling authentication between dotCMS and Zapier 
*/

'use strict';

/**
 * Sends data to the dotZapier plugin on dotCMS to perform workflow action
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @return String User-Friendly name for App Authentication details
 */
const getConnectionLabel = async (z, bundle) => {
  return bundle.inputData.givenName + ' ' + '[' + bundle.authData.email + ']';
};

/**
 * Invokes dotCMS Authentication API using basic authentication
 * to obtain the API key. This API key will be used for all future 
 * communication with the dotCMS plugin
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @return Dictionary Contains the API key. This will be stored as a 
 *                    derived attribute in the zapier bundle
 */
const generateApiKey = async (z, bundle) => {
  const options = {
    method: 'POST',
    url: `${bundle.authData.url}/api/v1/authentication/api-token`,
    headers: {
      'content-type': 'application/json',
      'accept': 'application/json'
    },
    body: {
      user: bundle.authData.email,
      password: bundle.authData.password,
      expirationDays: 730,
      label: 'Zapier Integration'
    }
  };

  return z.request(options).then((response) => {
    response.throwForStatus();

    const data = response.json;
    const apiKey = data.entity.token;

    return {
      apiKey: apiKey
    };
  });
};

/**
 * Invokes dotCMS current user API using the generated API key
 * Obtains the user details. This API must return 200 status code
 * for Zapier to store the API key generated from the authentication method
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @return Dictionary Contains User details. This will be stored as a 
 *                    derived attribute in the zapier bundle
 */
const getCurrentUser = async (z, bundle) => {
  const options = {
    url: `${bundle.authData.url}/api/v1/users/current`,
    method: 'GET'
  };

  return z.request(options).then((response) => {
    response.throwForStatus();

    const results = response.json;

    return results;
  });
};

/**
 * Zapier authentication Object
 * Session authentication is enabled for dotCMS
 * URL, email and password are required fields that needs to be inputed 
 * when the app is integrated with Zapier
 * API key authentication cannot be used as it does not allow one to store 
 * derived attributes 
 */
const authentication = {
  type: 'session',
  fields: [
    {
      computed: false,
      key: 'url',
      required: true,
      label: 'dotCMS URL',
      type: 'string',
      helpText: 'URL of the dotCMS Instance, for example: [Demo Site](https://demo.dotcms.com)'
    },
    {
      computed: false,
      key: 'email',
      required: true,
      label: 'Email',
      type: 'string',
      helpText: 'Email Address of the Administrator of the dotCMS instance, for example: [admin@dotcms.com](https://demo.dotcms.com)',
    },
    {
      computed: false,
      key: 'password',
      required: true,
      label: 'Password',
      type: 'password',
      helpText: 'The password of the dotCMS Administrator',
    }
  ],
  sessionConfig: {
    perform: generateApiKey,
  },
  test: getCurrentUser,
  connectionLabel: getConnectionLabel
};

module.exports = {
  authentication: authentication
}
