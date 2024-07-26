# Classification definitions

A list of classification labels can be specified, which then can be selected effortlessly by the user a the web-interface. This way, it prevents having different notations of the same thing in the meta data if any user types the labels. If a thing is not in that classification list, it is always possible for the users to type the label themselves.

---

In [**PhotoDB config**](config_photodb.md) `config.yaml` the classification definition file is set, e.g.:

```yaml
classification_definition_csv: photo_classification_definitions.csv
```

`photo_classification_definitions.csv` file example:

```CSV
#comment
name,description
incorrect box, Box does not mark (correctly) an object.
person, Photo will be locked. (DSGVO)
animal, unspecified animal
Kleintier, unbestimmt
Großtier, unbestimmt
Katze, generisch
Hauskatze, Felis catus
Wildkatze, Felis silvestris silvestris
#Hirsch, generisch
Wildschwein, Sus scrofa
Waschbär, Procyon lotor
```

The classification definition file is in **CSV format** with comma separated columns `name` and `description`.

`name` will be used as classification label that can be set at the web-interface, and that is stored in the photo meta data. Short names are preferred.

`description` specifies the exact meaning of `name`. It is shown at the web-interface classification label selection control as descriptive annotation.

To **disable a line** in the CSV file set the `#` character at the beginning of a line to comment out that line.