# Troubleshooting

Known issues and their solutions are listed on this page.  
If you found an issue not listed here you may [post a new issue](https://github.com/Nature40/audiodb/issues).

## PhotoDB

### Externally, image meta data YAML files changed but not reflected in PhotoDB

*Image metadata is rescanned for changes at request only.*  
Start the task [photo_refresh](photodb_tasks.md) to initiate a metadata rescan. Also at application restart metadata is rescanned.

## PhotoApp

### Image browser page shows placeholders instead of images

*There is a known issue changing to image browser page some time showing no images.*  
Move the mouse wheel to refresh the view. Then the images will show.

### Image browser page slowly loads all thumbnail images

*The thumbnail images may be not in the thumbnail image cache.*  
New images are processed and stored in the cache by task [photo_update_thumbs](photodb_tasks.md).
