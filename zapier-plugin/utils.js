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
    const triggerUrl = bundle.targetUrl;

    const options = {
      url: bundle.authData.url + dotZapierPluginUrl +  'subscribe',
      method: 'POST',
      body: {
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
    const options = {
      url: bundle.authData.url + dotZapierPluginUrl +'unsubscribe' + '?triggerName=' + bundle.subscribeData.id,
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
 * dotCMS content object returned by the list operation
 */
const sampleObject = {
  id: "2a4fb69a-1f79-4bef-be47-20dec8669c78",
  identifier: "2a4fb69a-1f79-4bef-be47-20dec8669c78",
  hostName: "demo.dotcms.com",
  url: "https://demo.dotcms.com/content.ec5c6e2f-4266-4ff8-adfc-22f76ba453b7",
  contentType: "My Blog",
  title: "French Polynesia Everything You Need to Know",
  modUserName: "Admin User",
  owner: "dotcms.org.1",
  archived: false,
  working: true,
  locked: false,
  live: true,
  modDate: "2022-06-07 18:23:10.844"
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