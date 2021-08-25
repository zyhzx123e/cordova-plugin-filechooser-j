# Cordova FileChooser Plugin

[Link to Developer](https://github.com/zyhzx123e/)

Requires Cordova >= 2.8.0

## Install with Cordova CLI
	$ ionic cordova plugin add cordova-plugin-filechooser-alwaysmkt
 
## API

```javascript
fileChooser.open(filter, successCallback, failureCallback); // with mime filter

fileChooser.open(successCallback. failureCallback); // without mime filter
```

### Filter (Optional)

```javascript
{ "mime": "application/pdf" }  // text/plain, image/png, image/jpeg, audio/wav etc
```

The success callback gets the uri of the selected file

```javascript
fileChooser.open(function(uri) {
  alert(uri);
});
```

## Screenshot

![Screenshot](filechooser.png "Screenshot")

## Supported Platforms

- Android
- Windows (UWP)

TODO rename `open` to pick, select, or choose.
