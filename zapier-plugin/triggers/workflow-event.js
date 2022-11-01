/**
 * Contains methods for Un Publish Zap Trigger
*/

const utils = require('../utils');

// Fetches a list of records from the endpoint
const perform = async (z, bundle) => {
    // Ideally, we should poll through all the pages of results, but in this
    // example we're going to omit that part. Thus, this trigger only "see" the
    // people in their first page of results.
    const response = await z.request({ url: 'https://swapi.dev/api/people/' });
    let peopleArray = response.data.results;
  
    if (bundle.inputData.species_id) {
      // The Zap's setup has requested a specific species of person. Since the
      // API/endpoint can't perform the filtering, we'll perform it here, within
      // the integration, and return the matching objects/records back to Zapier.
      peopleArray = peopleArray.filter((person) => {
        const speciesID = extractID(person.species[0]);
        return speciesID === bundle.inputData.species_id;
      });
    }
  
    return peopleArray.map((person) => {
      person.id = extractID(person.url);
      return person;
    });
  };


/**
 * Exports to index.js to initialize the trigger
*/
module.exports = {
    key: 'workflowEvent',
    noun: 'Content',
    display: {
        label: 'Content Workflow Event',
        description: 'Triggers on content workflow event.'
    },


    operation: {
        inputFields: [
            {
              key: 'contentType',
              type: 'string',
              helpText: 'Content Type Variable',
              dynamic: 'contentTypes.id',
              altersDynamicFields: true,
            }
        ],
        perform,
        type: 'hook',

        perform: utils.triggerOperation,
        performList: utils.listOperation,
        performSubscribe: subscribeOperation,
        performUnsubscribe: utils.unsubscribeOperation,
        
        sample: utils.sampleObject,
        outputFields: () => { 
            
            const contentTypes = inputFields[0].contentTypes;
            return utils.obtainOutputFields;



         }
    }
};
