# Image metadata

Metadata of images are stored as sidecar files in YAML format.

E.g. the metadata to an image-file `image01.jpg` is stored in the YAML-file `image01.jpg.yaml` located in the same folder or in a separate metadata folder structure of same structure as the image folders.  
*See [PhotoDB configuration properties root_path and root_data_path](config_photodb.md)*.

Example of `image01.jpg.yaml` file content:
```yaml
PhotoSens: v1.0
file: image01.jpg
file_size: 365532
XXH64: 3da0cef0a449c42a
width: 800
height: 600
location: loc_A
date: 2024-01-30T23:30:59
log:
- action: create yaml
  date: 2024-01-30T23:40:02
- action: generate jpg metadata
  date: 2024-01-30T23:40:04  
- action: run MegaDetector
  date: 2024-01-30T23:52:10
  contains_person: no
detections:
- bbox: [0.52, 0.46, 0.17, 0.32]
  classifications:
  - {classification: 'animal', classificator: MegaDetector, identity: 'v1.1', date: '2024-01-30T23:52:09', conf: 0.9}  
  - {classification: 'wild boar', classificator: Expert, identity: first_name.last_name, date: '2024-02-01T10:29:05'}
```

## Property specification

**PhotoSens**  
Signature of image metadata needed to identify the file as image metadata. Currently only allowed value: ```PhotoSens: v1.0```

**file**  
Filename of the image file belonging to the meta data. Value is the image file name.  

**file_size**  
Size of the image file in bytes.

**XXH64**  
A file hash of the image file content.  
*See [task photo_create_file_hashs](photodb_tasks.md)*.

**width**  
Image width in pixel.

**height**  
Image height in pixel.

**location**  
Location identifier of the image.

**date**  
Timestamp the image was taken. Format: ```YYYY```-```MM```-```DD```T```hh```:```mm```:```ss```

**log**  
List of entries documenting actions that where performed on the metadata. Properties of the log entries: 
- **action** Short description of the action
- **date** timestamp the action was performed, more properties may be added specific to the action.

**detections**

List of detection entries of the image. Properties of the detection entries:

- **bbox** Bounding box of the detected item on the image.
- **classifications** List of classification entries of the detection. Properties of the classification entries:
  - **classification** Classification label. 
  - **classificator** Classification type. 
  - **identity** Classification identity.
  - **date** Timestamp the classification was made.
  - **conf** The confidence rating of the classification.
