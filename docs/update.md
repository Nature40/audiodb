# Updating

First, make sure a running instance of the **application server is stopped**, e.g. by pressing crtl-c in the terminal.

Follow the **update procedure**. 

**Your** data files, meta data files, configuration **files will not be changed**.

After the update, **start the application** again.

# Updating on Ubuntu by script

Run the **update script** to download the newest release from the GitHub repository and update the application folders and files:

```bash
./github_update.sh
```

If some major structure changed, you may need to **clear the application cache**, the cache will **regenerate** at next application start:

```bash
./clear_cache.sh
```

# Updating manually

**Download** the latest release from the GitHub repository releases: 

[**https://github.com/Nature40/audiodb/releases**](https://github.com/Nature40/audiodb/releases)

[**Direct download of latest release**](https://github.com/Nature40/audiodb/releases/latest/download/package.zip)

**Extract** the downloaded zip file package.

At your application deployment folder, delete (or, for backup purposes, move them to a different location) and replace with from the downloaded release following files and folders:

**Folders**
* lib
* mustache
* webcontent

**Files**
* audio.jar

If some major structure changed, you may need to **clear the application cache**, the cache will **regenerate** at next application start. 
At application folder, files ending with ".db" are cache files to be deleted, e.g.:
* photo_cache.mv.db
* sample_meta.mv.db
* thumb_cache.mv.db

On Ubuntu, this file deleting task can be run by the script ```clear_cache.sh```