# PhotoDB tasks

Possibly long running actions within PhotoDB server are called tasks.

Currently tasks can be executed in the audio web-interface. Even for PhotoDB only tasks, you need to open the audio web-interface to manage tasks.

---

**Example**: Application sever is accessible by web address:
```text
http://localhost:8080
```
Then PhotoApp is at:
```text
http://localhost:8080/web/photo
```

Then to switch to AudioApp, you need to replace the `photo` part with `audio`:
```text
http://localhost:8080/web/audio
```
There, click on the top left button to open the left navigation side panel and select the entry `Task submission` to execute PhotoDB tasks.

---

## Tasks

---

**photo_create_file_hashs**

For all photo files create checksums, skip files with already created checksums.  
*See [image metadata property XXH64](image_metadata.md)*. 

---

**photo_create_yaml**

Traverse `root_data_path` and, for all jpg files without YAML file in root_path, create a new YAML file.  
*See [image metadata](image_metadata.md)*.

---

**photo_insert_megadetector_detections**

Insert MegaDetector detections.

MegaDetector retuns a JSON result files. Detections content of that file can be inserted in PhotDB meta data YAML files by this task.  
*See [image metadata property detections](image_metadata.md)*.

---

**photo_refresh**

Traverse `root_path` and check for changed or added or removed YAML files to update photo database.  
*See [PhotoDB configuration properties root_path and root_data_path](config_photodb.md)*.

This task needs to be run if some data was changed external, e.g. photo files have been added or YAML meta data has been modified manually.

---

**photo_update_thumbs**

For quick overview PhotoApp web-interface at the browser page shows small thumbnails of the photo files. For quick user interaction that thumbnails are cached and generated on demand.

This tasks scans for not already cached thumbnails and creates all missing thumbnails. This prevents the user to wait for viewing thumbnails at the browser page.

---