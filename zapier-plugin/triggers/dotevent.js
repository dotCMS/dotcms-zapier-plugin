/**
 * Contains methods for Save Zap Trigger
*/

const utils = require('../utils');

const triggerName = 'DotEvent';

/**
 * Fields displayed to the user at the time of Zap Action creation
 * choices: { Vanityurl: 'Vanityurl', FileAsset: 'FileAsset', MyBlog: 'MyBlog' },
 * choices: { edit: 'edit', unpublish: 'unpublish', publish: 'publish' },
 */
const  myinputFields = async (z, bundle) => {


    const response2 = await z.request(bundle.authData.url + '/api/v1/dotzapier/perform-allowed-type-list');
    response2.throwForStatus();
    var json2 = response2.json; // { myBlog: 'My Blog', event: 'Event' };


    return [
        {
            key: 'contentType',
            label: 'Content Type',
            type: 'string',
            helpText: 'Content Type to use in the operation',
            required: true,
            list: false,
            choices: json2,
            altersDynamicFields: false,
        }];
};

/**
 * Sends out the REST URL to dotCMS which is to be invoked for every trigger
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @return Dictionary Containing the triggerName as id. It will be used in the 
 *                    unsubscribe operation This will be stored as a 
 *                    derived attribute in the zapier bundle.
 */
const subscribeOperation = async (z, bundle) => {
    return utils.subscribeOperation(z, bundle, triggerName);
}

/**
 * Exports to index.js to initialize the trigger
*/
module.exports = {
    key: triggerName,
    noun: 'Contentlet',
    display: {
        label: 'dotCMS User-Defined Event ',
        description: 'Triggers when the Send to Zapier workflow sub-action is fired.',
        hidden: false,
        important: true,
    },

    operation: {
        type: 'hook',
        inputFields: [
            myinputFields,
        ],
        perform: utils.triggerOperation,
        performList: utils.listOperation,
        performSubscribe: subscribeOperation,
        performUnsubscribe: utils.unsubscribeOperation,
        
        sample: utils.sampleObject,
        outputFields: utils.sampleOutputFields
    }
};
