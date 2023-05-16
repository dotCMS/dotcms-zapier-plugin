/**
 * Contains Utility methods for the Zapier Triggers
*/

const dotZapierPluginUrl ='/api/v1/dotzapier/'

/**
 * Prepares the trigger request to be sent out. It is invoked 
 * for every trigger request. Encodes the url query parameters
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @returns Request object URL-Encoded request
*/
const triggerOperation = async (z, bundle) => {
    return [bundle.cleanedRequest];
};

/**
 * Zapier subscribe operation. Invoked when a Zap is published
 * Sends out the REST URL to dotCMS which is to be invoked for every trigger
 * This REST URL needs to be stored by dotCMS
 * The subscribe to unsubscribe mapping is handled by Zapier
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @param triggerName Name of the trigger for which the zap is to be triggered
 * @return Dictionary Containing the triggerName as id. It will be used in the 
 *                    unsubscribe operation This will be stored as a 
 *                    derived attribute in the zapier bundle.
 */
const subscribeOperation = async (z, bundle, triggerName) => {

    const triggerUrl  = bundle.targetUrl;
    const contentType = bundle.inputData.contentType;

    const options = {
      url: bundle.authData.url + dotZapierPluginUrl +  'subscribe',
      method: 'POST',
      body: {
        type: contentType,
        url: triggerUrl,
        triggerName: triggerName,
      }
    };
    
    return z.request(options).then((response) => {
      response.throwForStatus();
      const responseData = response.json;

      const results = {
        id: triggerName
      };
      return results;
    });
};

/**
 * Zapier unsubscribe operation. Invoked when a Zap is unpublished
 * Indicates to dotCMS that it will not accept any more triggers
 * for this specific operation. The REST URL sent earlier needs 
 * to be deleted by dotCMS
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @return Dictionary Contains the response from dotCMS. 
 *                    This data will not be used anywhere. 
 *                    It was added as Zapier expects data to be returned 
 *                    by this operation. An Empty dictionary throws an error
 */
const unsubscribeOperation = async (z, bundle) => {

    const triggerUrl = bundle.targetUrl;

    const options = {
      url: bundle.authData.url + dotZapierPluginUrl +'unsubscribe' + '?triggerUrl=' + triggerUrl,
      method: 'DELETE'
    };
  
    return z.request(options).then((response) => {
        response.throwForStatus();
        const results = response.json;
        return results;
    });
};

/**
 * Gets the most recent content from the dotCMS.
 * Zapier will display only the top 3. This operation expects the
 * response to be a list and every object in the list to have "id"
 * field
 * @param z Zapier object
 * @param bundle Stores all the user input as well derived attributes 
 * @return List  Contains the most recent dotCMS content
 */
const listOperation = async (z, bundle) => {
    const options = {
      url: bundle.authData.url + dotZapierPluginUrl + 'perform-list',
      method: 'GET'
    };
  
    return z.request(options).then((response) => {
        response.throwForStatus();
        const responseData = response.json
        const results = responseData.data;
        return JSON.parse(results);
    });
};




/**
 * Extracts the keys from the dotCMS content object
 * @param obj dotCMS content object
 * @returns List Contains keys to be displayed to the user on Zapier action
 */
const obtainOutputFields = (obj) => {
  const fields = [];

  for(const key in obj) {
    fields.push({
      key: key
    })
  }

  return fields;
};

/**
 * dotCMS content object returned by the list operation
 */
const sampleObject = {

    "BlockEditorDoc.id": "da1c6bdd51afe6ed9951302c9441d12f",
    "BlockEditorDoc.identifier": "da1c6bdd51afe6ed9951302c9441d12f",
    "BlockEditorDoc.hostName":"48190c8c-42c4-46af-8d1a-0cd5db894797",
    "BlockEditorDoc.contentType": "BlockEditorDoc",
    "BlockEditorDoc.title": "Test4",
    "BlockEditorDoc.modUserName": "dotcms.org.1",
    "BlockEditorDoc.owner": "dotcms.org.1"
};

const sampleOutputFields = obtainOutputFields(sampleObject);

module.exports = {
  triggerOperation: triggerOperation,
  subscribeOperation: subscribeOperation,
  unsubscribeOperation: unsubscribeOperation,
  listOperation: listOperation,
  sampleObject: sampleObject,
  sampleOutputFields: sampleOutputFields,
  dotZapierPluginUrl: dotZapierPluginUrl
}
