# Configuration

Application settings are in YAML file `config.yaml`

# Application configuration

`config.yaml` example:
```yaml
login: true
http_port: 0
https_port: 8000
keystore_path: 'certificate_with_certificate_chain.p12'
keystore_password: 'myPassword'
```

# Configuration YAML properties

**login**

No login needed, no access restrictions:
```yaml
login: false
```

Login needed:
```yaml
login: true
```
---

**http_port**

Application server listening on plain HTTP port.

Default port 8080:
```yaml
http_port: 8080
```

Disable HTTP, not listening on HTTP port:
```yaml
http_port: 0
```
---

**https_port**

Application server listening on encrypted secure HTTPS port. Needs keystore file at `keystore_path` and password at `keystore_password`.

Default port 8000:
```yaml
https_port: 8000
```

Disable HTTPS, not listening on HTTPS port:
```yaml
https_port: 0
```
---

**keystore_path**

*Needed for HTTPS connections only. Not needed for HTTP connections, so no keystore file. In the distribution package, no keystore file is included.*

Valid certificate for HTTPS encryption. Needs correct password at `keystore_password`.

Default `keystore.jks` filename at application root folder:
```yaml
keystore_path: 'keystore.jks'
```

Keystore should be in standardized [**PKCS #12**](https://en.wikipedia.org/wiki/PKCS_12) format, containing signed certificate, public key, encrypted private key and full certificate chain:
```yaml
keystore_path: 'certificate_with_certificate_chain.p12'
```
*See [HTTPS certificates](https_certificates.md)*.

---

**keystore_password**

*Needed for HTTPS connections only. This password opens the keystore file. This password has no relation to user login accounts.*

Password for keystore. Needed by `keystore_path`.

Setting correct password needed to decrypt the keystore file at keystore_path:
```yaml
keystore_password: 'myPassword'
```
---