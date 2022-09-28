/**
 * Contains methods for CMD Operation Zap Action 
*/

'use strict';

const utils = require('../utils');

/**
 * Fields displayed to the user at the time of Zap Action creation
*/
const inputFields = [
    {
        key: 'contentType',
        label: 'Content Type',
        type: 'string',
        helpText: 'Content type of the Content that is to be published. It is required for the save action',
        required: false,
        list: false,
        altersDynamicFields: false,
    },
    {
      key: 'text',
      label: 'Text',
      type: 'string',
      helpText: 'Text of the Content that is to be published',
      required: true,
      list: false,
      altersDynamicFields: false,
    }
];

/**
 * Sends data to the dotZapier plugin on dotCMS to perform workflow action
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @return Dictionary JSON response from dotZapier plugin API
 */
const createAction = async (z, bundle) => {  
    const body = {
        contentType: bundle.inputData.contentType,
        text: bundle.inputData.text
    }
  
    const options = {
      url: bundle.authData.url + utils.dotZapierPluginUrl + 'action',
      method: 'POST',
      body: body
    };

    return z.request(options).then((response) => {
        response.throwForStatus();
        const results = response.json;
        return results;
    });
}

/**
 * Exports to index.js to initialize the create action
*/
module.exports = {
    key: 'cmdOperation',
    noun: 'Content',
    display: {
        label: 'Perform an action on dotCMS',
        description: 'Perform content manipulation on dotCMS',
        hidden: false,
        important: true,
    },

    operation: {
        perform: createAction,

        inputFields: inputFields,

        sample: utils.sampleObject,
        outputFields: utils.sampleOutputFields
    }
};