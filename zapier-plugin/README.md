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
publish,
unpublish,
archive,
unarchive
```

Hashtags are used for the different attributes on the `Content API`

The content type is specified at the time of the creation of the Zap. This could be hardcoded on the Zap itself or one can specify it dynamically via the Zapier app actions. Each Zapier application has its own set of actions that could be used to specify the content type.

Within the command, double quotes after equal to (=) are optional if the value associated with it does not contain any whitespace character. All text which is not associated with any hashtag is considered as Body. The hashtags are dynamic and any attribute can be used as a hashtag

### Examples 

```
#save #title="First Content Title" #url=first-content-title #author="John Doe" #publishDate="Sept 28 2022" Hello World

In the above command, the "Save" action is invoked via the CRUD API with the following payload
{
    "actionName": "save",
    "comments": "Performed by Zapier",
    "contentlet": {
        "contentType": "My Blog", // Content Type Name
        "title": "My First Content Title",
        "body": "Hello World",
        "author": "John Doe",
        "url-title": "my-first-content-title",
        "publishDate": "2022-09-28 00:00:00"
    }
}
```

```
1. #publish #id=123

2. #publish #url=first-content-title

In the above commands, the "Publish" action is invoked via the CRUD API with the following payload

{
    "actionName": "publish",
    "comments": "Performed by Zapier",
}

The query parameter "identifier" contains the content identifier. 

Currently one can specify either the content identifier or the url-title within the text. The url-title will be translated to the content identifier  which is used as the query parameter to the above mentioned API invocation.
```

Following date time formats are supported. They are listed below in the order of priority 

```
MM dd yy 
dd MM yy
dd yy MM
MM yy dd
yy MM dd
yy dd MM
MM dd yyyy
dd MM yyyy
dd yyyy MM
MM yyyy dd
yyyy MM dd
yyyy dd MM
```

As for time, 24 Hour format [HH:mm:ss] is only supported. The default time is 00:00:00. If partial time is provided then, the skipped parameters default to `00`
