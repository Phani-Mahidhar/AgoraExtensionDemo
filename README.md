# Marsview Agora Android Extension

Marsview's Agora Extension provides Speech Analytics which enables the developers to get the following output:

- SPEECH-TO-TEXT
- EMOTION & TONE
- SENTIMENT
- CONVERSATION TYPE
- QUESTIONS & RESPONSES  
- ACTION ITEMS & FOLLOW UPS  
- TOPICS
- SUMMARY  
- SPEECH INSIGHTS

<hr/>  

# Marsview Agora Android Extension

This tutorial helps you to quickly get started with using Marsview's Agora extension in your Android application.  

## Prequisites

* Java version 8.
* Physical Android device(Android Oreo or Higher) with USB Debugging enabled (A physical device is required for debugging as android emulators lack some functionality to run the extension).  
* Android Studio 3.3 or above.  
* Agora Account.  

## Quick Start
This section guides you through on how to prepare, build and run the sample application.

### Obtain an appId

To build and run the sample application you require an appId, which can be acquired by following these steps:

Create a developer account at agora.io. Once you finish the signup process, you will be redirected to the Dashboard.

Navigate in the Dashboard tree on the left to Projects > Project List.

Save the appId from the Dashboard for later use.

Generate a temp Access Token (valid for 24 hours) from dashboard page with given channel name, save for later use.

Open the AgoraExtensionDemo/app/src/main/res/values/strings.xml file and
place your App Id, Agora Channel Name, Agora Temp Access Token in their respective variables.

```
<string name="agora_app_id">Agora APP ID</string>
<string name="agora_channel">Agora CHANNEL NAME</string>
<string name="agora_access_token">Agora ACCESS TOKEN</string>
```

### Obtain Project API Key and API Secret
When you add the Marsview extension to your agora project, you will receive an API KEY and API SECRET from Marsview for that particular project.  

Paste these in AgoraExtensionDemo/app/src/main/res/values/strings.xml file
in the respective variables.

```
<string name="marsview_apiKey">PROJECT API KEY</string>
<string name="marsview_apiSecret">PROJECT API SECRET</string>
```

store the API_KEY and API_SECRET in a variable in the MainActivity.java file.

```
public String API_KEY = getString(R.string.marsview_apiKey);
public String API_SECRET = getString(R.string.marsview_apiSecret);
```

## Usage

Once you disable the extension in the UI, you will receive a transactionId corresponding to the audio file uploaded.  
This transactionId is used to POST compute data to the Marsview Servers to enable processing of the audio data.  

The compute Model can be found in the onEvent Method of MainActivity.java file

By default this is the model posted in for compute data.
```

    final JSONArray enableModels = new JSONArray("[{
        'modelType':'speech_to_text', 
        'modelConfig': {
            'automatic_punctuation': true,      
            'custom_vocabulary': ['Marsview', 'Communications'], 
            'enableKeywords': true, 
            'enableSuggestedIntents': true, 
            'topics': {'threshold': 0.5}
            }
        }]");


```

Other Compute Models can be posted and can be referenced from [the marsview docs page](https://docs.marsview.ai/).  


## Run the Application

Open the project with Android Studio, Connect your Androd device, build and run the sample application.  







