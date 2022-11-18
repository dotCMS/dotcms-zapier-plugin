/**
 * Contains methods for CMD Operation Zap Action 
*/

'use strict';

const utils = require('../utils');

/**
 * Fields displayed to the user at the time of Zap Action creation
 * choices: { Vanityurl: 'Vanityurl', FileAsset: 'FileAsset', MyBlog: 'MyBlog' },
 * choices: { edit: 'edit', unpublish: 'unpublish', publish: 'publish' },
 */
const  actionField = async (z, bundle) => {

    const options = {
        url: bundle.authData.url + dotZapierPluginUrl + 'perform-action-list',
        method: 'GET'
    };

    const response = await z.request(options);

    response.throwForStatus();

    return [{
        key: 'actionName',
        label: 'Action',
        type: 'string',
        helpText: 'Action to execute',
        required: true,
        list: false,
        choices: JSON.parse(response.json),
        altersDynamicFields: false,
    }];
};

const  typeField = async (z, bundle) => {

    const options = {
        url: bundle.authData.url + dotZapierPluginUrl + 'perform-type-list',
        method: 'GET'
    };

    const response = await z.request(options);

    response.throwForStatus();

    return [{
            key: 'contentType',
            label: 'Content Type',
            type: 'string',
            helpText: 'Content type to use on the operation',
            required: true,
            list: false,
            choices: JSON.parse(response.json),
            altersDynamicFields: false,
    }];
};

const  xmyinputFields = async (z, bundle) => {

    const options = {
        url: bundle.authData.url + dotZapierPluginUrl + 'perform-type-list',
        method: 'GET'
    };

    const response = await z.request(options);

    response.throwForStatus();

    return [{
        key: 'contentType',
        label: 'Content Type',
        type: 'string',
        helpText: 'Content type to use on the operation',
        required: true,
        list: false,
        choices: JSON.parse(response.json),
        altersDynamicFields: false,
    }];
};

const  myinputFields = async (z, bundle) => {

    const response1 = await z.request(bundle.authData.url + '/api/v1/dotzapier/perform-action-list');

    response1.throwForStatus();
    var json1 = response1.json; //{ save: 'Save', publish: 'Publish' };

    const response2 = await z.request(bundle.authData.url + '/api/v1/dotzapier/perform-type-list');
    response2.throwForStatus();
    var json2 = response2.json; // { myBlog: 'My Blog', event: 'Event' };

    return [
        {
            key: 'actionName',
            label: 'Action',
            type: 'string',
            helpText: 'Action to execute',
            required: true,
            list: false,
            choices: json1,
            altersDynamicFields: false,
        },
        {
            key: 'contentType',
            label: 'Content Type',
            type: 'string',
            helpText: 'Content type to use on the operation',
            required: true,
            list: false,
            choices: json2,
            altersDynamicFields: false,
    }];
};

/**
 * Sends data to the dotZapier plugin on dotCMS to perform workflow action
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @return Dictionary JSON response from dotZapier plugin API
 */
const createAction = async (z, bundle) => {  
    const body = {
        contentType: bundle.inputData.contentType,
        actionName: bundle.inputData.actionName,
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
        label: 'Perform An Action On DotCMS',
        description: 'Perform content manipulation on dotCMS',
        hidden: false,
        important: true,
    },

    operation: {
        perform: createAction,

        inputFields: [
            {
                key: 'text',
                label: 'Text',
                type: 'string',
                helpText: 'Text of the Content that is to be published',
                required: true,
                list: false,
                altersDynamicFields: false,
            },
            myinputFields,
        ],

        sample: utils.sampleObject,
        outputFields: utils.sampleOutputFields
    }
};
