# HTTPS certificates

*For HTTP connections, no certificates are needed.*

For encrypted and certified **HTTPS** connections a valid certificate has to be provided in the [**PKCS #12**](https://en.wikipedia.org/wiki/PKCS_12) format.

*See [config](config.md)*.

***

If you have the **PKCS #12** certificate already, you may use the `openssl` commands to check your certificate for correctness, e.g. correct password.

How to create a suitable certificate file depends on the certificate you have. The following steps may guide you from a typically used [**X.509**](https://en.wikipedia.org/wiki/X.509) certificate with separate certificate **root chain** to the encrypted **PKCS #12** certificate file.


Following intput files are presumed:
- **key.pem** private key
- **cert-NAME.pem** certificate of the key
- **chain.pem** root certificate chain of the certificate


```bash
# Show signed zertificate.
openssl x509 -in cert-NAME.pem -text -noout

# Show root zertificate chain.
openssl x509 -in chain.pem -text -noout > text.txt

# Verify certificate with chain.
openssl verify -CAfile chain.pem cert-NAME.pem

# Merge root zertificate chain and signed zertificate.
cat chain.pem cert-NAME.pem > cert-NAME-chain.pem

# Show certificate with root chain.
openssl x509 -in cert-NAME-chain.pem -text -noout

# Export to pkcs12.
openssl pkcs12 -export -clcerts -inkey key.pem -in cert-NAME-chain.pem -out cert-NAME-full.p12 -name "Name"

# Show pkcs12.
openssl pkcs12 -info -in cert-NAME-full.p12
```


***

**Alternatively** to HTTPS management inside of the application server, you may consider to use an external application to manage certificates and apply HTTPS transformation to HTTP connections, e.g. with an **application proxy** [Traefik](https://doc.traefik.io/traefik/) or HTTPS transformation services from your institution network management.