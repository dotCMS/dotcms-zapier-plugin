/**
 * Contains methods for CMD Operation Zap Action 
*/

'use strict';

const utils = require('../utils');


const contentTypeFields = async (z, bundle) => {

    try {

        const response3 = await z.request(bundle.authData.url + '/api/v1/dotzapier/get-content-type-fields?contentType=' + bundle.inputData.contentType);
        response3.throwForStatus();
        var contentTypeFieldsJson = response3.json; // { fieldVariable1: 'field Name1', fieldVariable2: 'field Name2' };
        var fields = [];
        for (var key in contentTypeFieldsJson) {
            fields.push({ key: key, label: contentTypeFieldsJson[key] });
        }

        return { key: 'contentTypeFields', children: fields };
    } catch (e) {

        return [{ key: 'contentTypeFields',
            children: [
                {
                    key: 'zapierJson',
                    label: 'Text',
                    type: 'string',
                    helpText: 'Json of the Content that is to be published',
                    required: false,
                    list: false,
                    altersDynamicFields: false,
                },
            ],
        }];
    }
};

/**
 * Fields displayed to the user at the time of Zap Action creation
 * choices: { Vanityurl: 'Vanityurl', FileAsset: 'FileAsset', MyBlog: 'MyBlog' },
 * choices: { edit: 'edit', unpublish: 'unpublish', publish: 'publish' },
 */
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
            helpText: 'Content type to use on the operation',
            required: true,
            choices: json2,
            altersDynamicFields: true,
        }
    ];
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
        actionName:  bundle.inputData.actionName,
        inputFormat: "json",
        text: formatContentTypeFieldValues(Object.keys(bundle.inputData.contentTypeFields || {}).length > 0
            ? bundle.inputData.contentTypeFields
            : bundle.inputData),
    }

    z.console.log("body: " + JSON.stringify(body));

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
 * Cleans the value string from special chars and replaces then with a dash
 *
 * @param {string} name
 * @returns string
 */
const escapeValue = name => {
    return name.replace(/\\n/g, "\\n")
        .replace(/\\'/g, "\\'")
        .replace(/\\"/g, '\\"')
        .replace(/\\&/g, "\\&")
        .replace(/\\r/g, "\\r")
        .replace(/\\t/g, "\\t")
        .replace(/\\b/g, "\\b")
        .replace(/\\f/g, "\\f");
}

/**
 * Formats the content type field values to be sent to dotCMS
 * @param contentTypeFields
 * @returns {{}}
 */
const formatContentTypeFieldValues = contentTypeFields => {

    let formattedContentTypeFieldValues = {};

    if (contentTypeFields) {
        for (let key in contentTypeFields[0]) {

            if (key === 'actionName' || key === 'contentType') {
                continue;
            }

            if (key === 'zapierJson') {

                return JSON.parse(contentTypeFields[0][key]);
            }

            formattedContentTypeFieldValues[key] = escapeValue(contentTypeFields[0][key]);
        }
    }
    return formattedContentTypeFieldValues;
};

/**
 * Exports to index.js to initialize the create action
*/
module.exports = {
    key: 'cmdOperation',
    noun: 'Content',
    display: {
        label: 'Perform an Action on DotCMS',
        description: 'Perform content manipulation on dotCMS',
    },

    operation: {
        perform: createAction,

        inputFields: [
            myinputFields,
            contentTypeFields,
        ],

        sample: utils.sampleObject,
        outputFields: utils.sampleOutputFields
    }
};
