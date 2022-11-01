/**
 * Contains methods for Un Publish Zap Trigger
*/

const utils = require('../utils');

// Fetches a list of records from the endpoint
const perform = async (z, bundle) => {
    let options = {
        url: bundle.authData.url + "/api/v1/contenttype?filter=" + bundle.inputData.contentType + "&orderby=modDate&direction=DESC&per_page=50",
        method: 'GET'
      };
    



    const response = await z.request(options);
    z.console.log
    let contentTypes = response.data.results.entity;

    return contentTypes.map((cType) => {
        cType.id = cType.variable;
        return cType;
      });



};
/**
 * Exports to index.js to initialize the trigger
*/
module.exports = {
    key: 'contentTypes',
    noun: 'Content Types',
    display: {
        label: 'List of Content Types',
        description: 'This is a hidden trigger, and is used in a Dynamic Dropdown of another trigger.',
        hidden: true,
    },


    operation: {
        // Since this is a "hidden" trigger, there aren't any inputFields needed
        perform,
        // The folowing is a "hint" to the Zap Editor that this trigger returns data
        // "in pages", and that the UI should display an option to "load more" to
        // the human.
        canPaginate: false,
      }
};
