/**
 * Contains methods for Un Publish Zap Trigger
*/

const utils = require('../utils');

// Fetches a list of records from the endpoint
const perform = async (z, bundle) => {
    let options = {
        url: bundle.authData.url + "/api/v1/contenttype/id/" + bundle.inputData.contentType ,
        method: 'GET'
      };
    



    const response = await z.request(options);
    z.console.log
    let fields = response.data.results.entity.fields;

    fields = fields.filter((field) => {
      const fieldType = field.fieldType
      return fieldType != "Row" && fieldType!="Column";
    });


    return fields.map((field) => {
      field.id = field.variable;
        return field;
      });



};
/**
 * Exports to index.js to initialize the trigger
*/
module.exports = {
    key: 'fields',
    noun: 'Fields',
    display: {
        label: 'List of Fields',
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
