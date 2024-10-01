# User accounts

Users of the application are identified by account names.

The application can be run either with **login disabled** or **login enabled**.

## Login disabled

With [**config**](config.md) entry
```yaml
login: false
```
the application is used without the need for user login password procedure.
All users using the application are named by the account `anonymous`.

There are no access restrictions, so `anonymous` has admin rights.


## Login enabled


With [**config**](config.md) entry
```yaml
login: true
```
the application is used with the need for user login password procedure.

Logged in users have access restrictions specified by the **roles** they are associated to.

On the newly installed application, there are no accounts registered, so a login is not possible. Start with Login disabled to create at least one admin account. Then you can enable login and use your created admin account to login.


## Account management

*Accounts can be managed by users logged in by accounts with associated **admin** role only. Or by **disabled login**, leading to **admin rights**.*

The **account management page** is located at the AudioApp web interface.


Even for PhotoDB only projects, you need to open the AudioDB web interface to manage accounts.

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



At AudioApp, click on the top left button to open the left navigation side panel and select the entry `Project selection`. Then the left navigation side panel shows some entries. Click on the `Accounts` entry to open the **user accounts management page**.

By **create new account**-button, new user accounts can be created. Normal users of AudioDB or PhotoDB do not need roles. So the roles selection box can be left empty. A user account privileged to manage user accounts needs the role `admin` or some other more specific role.

## Roles

*Normal user accounts may not need roles.*

**Privileges** and **restrictions** are specified by roles that are associated to user accounts by account management.

Roles:
- `admin`: **Most privileged** role, contains `manage_account` and `create_account`.
- `manage_account`: Able to only **manage accounts**, contains `create_account`.
- `create_account`: Able to only **create accounts**.
- `readOnly`: Restriction to view but **not change**, currently AudioDB only.
- `reviewedOnly`: Restriction to only view **already reviewed** items, currently AudioDB only.