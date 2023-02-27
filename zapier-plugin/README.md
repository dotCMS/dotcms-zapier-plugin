# dotCMS Zapier

Zapier Integration for dotCMS. 

It allows the user to create content on dotCMS via Zapier

## Zaps

triggers => DotCMS to Zapier [Reads data from dotCMS API]

creates => Zapier to dotCMS [Sends data to dotCMS API]

## Zapier Actions

`zapier logs --type=http --detailed` View all HTTP 

`zapier logs` View Zap logs

`zapier push` Deploys the code to Zapier

## Node Requirements

Node: `>=v14`

NPM: `>=5.6.0`

Set the URL env to test such as:

export URL=https://local.dotcms.site

Test: `npm run test`

---

## dotCMS Commands

Supported dotCMS commands on the Zapier platform to operations on dotCMS

```
save,
edit,
publish,
unpublish,
archive,
unarchive,
delete
```

Hashtags are used for the different attributes on the `Content API`

The content type is specified at the time of the creation of the Zap. This could be hardcoded on the Zap itself or one can specify it dynamically via the Zapier app actions. Each Zapier application has its own set of actions that could be used to specify the content type.

Within the command, double quotes after equal to (=) are optional if the value associated with it does not contain any whitespace character. All text which is not associated with any hashtag is considered as Body. The hashtags are dynamic and any attribute can be used as a hashtag

### Examples 

```
// In this example the action publish a content called "ZapierBean" with a couple values.

curl --location --request POST 'https://demo.dotcms.io/api/v1/dotzapier/action' \
--header 'Authorization: Bearer eyJ0eXAiOiJK...64' \
--header 'Content-Type: application/json' \
--data-raw '{
    "contentType":"ZapierBean",
    "actionName":"publish",
    "inputFormat":"csv",
    "text":"title=New Zapier Bean, value=New Zapier Value"
}'


Another example but using json format instead of csv

curl --location --request POST 'https://demo.dotcms.io/api/v1/dotzapier/action' \
--header 'Authorization: Bearer eyJ0eXAiOiJK...64' \
--header 'Content-Type: application/json' \
--data-raw '{
    "contentType":"ZapierBean",
    "actionName":"publish",
    "inputFormat":"json",
    "text":"{\"title\":\"New Zapier Bean\", \"value\":\"New Zapier Value\"}"
}'
```


## App Configuration

Regarding the App configuration, the only optional parameter is a list of content types allowed use on Zapier
