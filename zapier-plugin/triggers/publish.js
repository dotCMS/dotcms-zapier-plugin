/**
 * Contains methods for Publish Zap Trigger
*/

const utils = require('../utils');

const triggerName = 'publish';

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
    noun: 'Content',
    display: {
        label: 'Publish Content',
        description: 'Triggers when new content is published.',
        hidden: false,
        important: true,
    },

    operation: {
        type: 'hook',

        perform: utils.triggerOperation,
        performList: utils.listOperation,
        performSubscribe: subscribeOperation,
        performUnsubscribe: utils.unsubscribeOperation,
        
        sample: utils.sampleObject,
        outputFields: utils.sampleOutputFields
    }
};
