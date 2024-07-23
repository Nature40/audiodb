# Quasar App (app)

A Quasar Framework app

## Install the dependencies
```bash
npm install
```

### Start the app in development mode (hot-code reloading, error reporting, etc.)

Workaround needed for error:
https://github.com/webpack/webpack/issues/15900
https://github.com/webpack/webpack/issues/14532

On Ubuntu, run before "quasar dev" or "quasar build":
```bash
export NODE_OPTIONS=--openssl-legacy-provider
```

On Windows, run before "quasar dev" or "quasar build":
```Batchfile
set NODE_OPTIONS=--openssl-legacy-provider
```


```bash
quasar dev
```

### Lint the files
```bash
npm run lint
```

### Build the app for production
```bash
quasar build
```

### Customize the configuration
See [Configuring quasar.conf.js](https://quasar.dev/quasar-cli/quasar-conf-js).
