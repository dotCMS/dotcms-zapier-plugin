# dotCMS Zapier JS Integration

This repo contains the code for the dotCMS Zapier Integration (hosted on Zapier's platform). 

It allows the user to use both Creates and Triggers in Zapier
1. Creates => Zapier Zap to dotCMS Content.  Use this to create/modify a dotCMS content object from Zapier.
2. Triggers => DotCMS content to Zapier Zap.  Use this to trigger an Zapier action when a dotCMS content object is modified. 

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

When "Zapping" content to dotCMS, we supported the dotCMS "Default Actions" - which can be selected in your Zap on the Zapier platform.  The following default actions are availble for your Zap.

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

Zapier sends dotCMS Content in a "text" field.  dotCMS can injest this formated either as a comma separated list or as String formatted JSON.

This is the example request made by this Zapier Plugin that publishes a content called "ZapierContent" with a title field and a value field.
```
curl --location --request POST 'https://demo.dotcms.io/api/v1/dotzapier/action' \
--header 'Authorization: Bearer eyJ0eXAiOiJK...64' \
--header 'Content-Type: application/json' \
--data-raw '{
    "contentType":"ZapierBean",
    "actionName":"publish",
    "inputFormat":"csv",
    "text":"title=New Zapier Bean, value=New Zapier Value"
}'
```

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
