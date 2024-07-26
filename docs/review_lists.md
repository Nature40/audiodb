# Review lists

Apart from directly traversing the project photo archive by location and time, **review lists** allow to traverse a subset of the photos in a sequential order. One review list contains a list of photo entries to traverse.

---

In [**PhotoDB config**](config_photodb.md) `config.yaml` the review list folder is set, e.g.:

```yaml
review_list_path: 'photo_review_lists'
```
The review list a folder that contains files that each specify a review list.

The review list files are in **CSV format** with comma separated columns. Currently the one column `path`.

The review list folder may contain a CSV file `manual_label_list.csv` e.g:

```CSV
path
photo2_20220101_020101.jpg
photo4_20220101_040101.jpg
```

To **disable a line** in the CSV file set the `#` character at the beginning of a line to comment out that line.

The `path` is the path to a photo file relative to the photo data root folder.

The CSV file name is the name of that review list, without the `.csv` extension.