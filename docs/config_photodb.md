# PhotoDB Configuration

Application settings are in YAML file `config.yaml`

`config.yaml` example:
```yaml
login: true
http_port: 8080
    
photo:
  projects:

  - project: my_photo_project
    root_path: 'photo_meta'
    root_data_path: 'photo_data'
    classification_definition_csv: photo_classification_definitions.csv
    review_list_path: 'photo_review_lists'
```

# PhotoDB project configuration YAML properties

**project**

Name of the project, e.g. shown at the top panel at PhotoApp web-interface:
```yaml
project: my_photo_project
```
---

**root_path**

Folder of project photo metadata files.

Folder relative to application root folder:
```yaml
root_path: 'photo_meta'
```
---

**root_data_path**

Folder of project photo files. If not set, photo files are expected to be in same folder as metadata files of `root_path`.

Folder relative to application root folder:
```yaml
root_data_path: 'photo_data'
```
---

**classification_definition_csv**

List of classification names as CSV file.

File relative to application folder:
```yaml
classification_definition_csv: photo_classification_definitions.csv
```
*See [classification definitions](classification_definition.md)*.

---

**review_list_path**

Folder of project review list files.

Folder relative to application folder:
```yaml
review_list_path: 'photo_review_lists'
```
*See [review lists](review_lists.md)*.

---