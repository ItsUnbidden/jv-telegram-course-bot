# How to set up the platform?
This is a section where I'll explain how to set things up correctly.

- [How to set up the platform?](#how-to-set-up-the-platform)
  - [How to register a new bot?](#how-to-register-a-new-bot)
  - [What about localizations?](#what-about-localizations)
  - [application.properties](#applicationproperties)
  - [How to launch the app?](#how-to-launch-the-app)
    - [Testing, local setup](#testing-local-setup)
    - [Deployment](#deployment)

## How to register a new bot?

The **future bot's** *creator* has to be registered on the platform. This can be done by pressing the **Start** button in at least one of the bots (**Initial Bot** is always available). They also need to register their new bot in the **Telegram ecosystem** with [*BotFather*](https://t.me/BotFather). After that's done, they need to communicate their *bot token* and optionally *internal name* to the *Director*, so that they can set the bot up.

## What about localizations? 
**Localizations** allow the platform to support *multiple languages* and prioritize them depending on what language is set as the user's **default Telegram language**. Users can also pick a language **manually**. Not only can text content be localized, **media content** like videos, audios, etc. can be localized too.

Currently, there are *some issues* with localizations that require additional communication between the *Creators* and the *Director*, specifically:
- Bot's **text content** cannot be set up in the bot itself — only through localization files.
- Bot's name, descriptions, and invoice messages must also be set up through localizations.

These issues are due to the app's initial concept supporting only one bot instead of multiple. **This will be fixed in the future.**

The *Director* can upload new localization files through the **Control Bot**, so they can be updated relatively easily.

Localizations are grouped in **separate files** by their type:
- `button.txt`
- `course.txt`
- `menu.txt`
- `error.txt`
- `service.txt`

These files are located in a folder **named by the language code** (ISO 639) of the language they represent, for example:
- `en`
- `ru`
- `de`

So if you want to support **several** languages, you need to write all localizations for that language in those five files specified above, and put them in the folder **corresponding to that language's code**.

These folders are located in a special folder, which is `\localizations` by default, but it can be configured in `application.properties` through the `telegram.bot.message.text.path` property.

You also have to set **language priority** for **all** your languages. It will not be set for you. Where to do this, you can read in the [next section](#applicationproperties).

Now, about localization syntax. It must look exactly like the example below, except that you put the mapping you need instead of `<get_archive_reviews>`:
```
<get_archive_reviews>
Archive reviews
<get_archive_reviews/>
```
The text between `<get_archive_reviews>` and `<get_archive_reviews/>` is the localization text. The important point to take here is that in cases where you need to import some values, like with the example below, you need to put `true` after the mapping name in the first `<>`:

```
<course_price_update_success true>
Course **${courseName}**'s price has been successfully changed to **${currentPrice}**⭐️.
<course_price_update_success/>
```

If you forget to put `true` in those `<>`, the values will not be imported. **Do not put `true` everywhere**, because that will consume additional resources due to how formatting works.

**Now about formatting**. Different markers for formatting text somewhat differ from Markdown:
- `**` — bold
- `__` — italic
- `--` — underline
- `~~` — strikethrough
- `^^` — spoiler

I know that this system is *not the greatest*, and there is a chance it will be revamped later in favor of *native Telegram formatting*.

**Mappings** for localizations can be found in `mappings.txt`. Default **localization files** for *English* are also available *in this repository*.

You will likely make *some syntax mistakes* while writing all of these localizations. If a localization file **cannot be parsed**, the app will **not launch** or give an error during refresh. You can check the cause of the error in the *logs*. I still recommend carefully checking all localizations in the Initial Bot to make sure they work correctly.

## application.properties
This file can be found in `\src\main\resources\application.properties`. It defines many different application-wide settings. I recommend leaving most things at their default values, but some settings you have to configure. Please be aware that if you're using **Docker**, you must also edit the `.env` file, since it supersedes some properties in `application.properties`.

**Database settings:**
```properties
spring.datasource.url=jdbc:mysql://example-db.com/telegram_courses_platform_db?serverTimezone=UTC
spring.datasource.username=username
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

You might also need to edit `liquibase.properties` if you are not using **Docker**.

**Logging level**
```properties
# Application logging level. Spring logging will be restricted to the default INFO level.
logging.level.com.unbidden.telegramcoursesbot=DEBUG
```
You might want to change this to a higher level, like ``INFO`` to avoid too detailed logs.

**Platform main access settings**
```properties
# Initial bot token
telegram.bot.authorization.start_bot.token=initial_bot_token

# Initial bot name
telegram.bot.authorization.start_bot.name=test_bot

# Bot father token
telegram.bot.authorization.bot_father.token=botfather_token

# Director id. Director has total authority over the application (cross-bot authority)
telegram.bot.authorization.director.id=telegram_user_id
```

These settings are mandatory and must be filled in in order for the app to work correctly.

**Localizations**
```properties
# Path to localization directories
telegram.bot.message.text.path=localizations

# Localization files format (should be like .txt).
telegram.bot.message.text.format=.txt

# Language codes priority (from most important to least important, example: en, ru). These 
# languages will only be used if user prefered localization is not available.
telegram.bot.message.language.priority=en
```

I don't recommend changing the first two settings, but you do you. The language priority depends on your situation. 

**Webhooks**
This is the most difficult part; if you get this wrong, things will not work. Please be careful.
```properties
# Secret key to ensure webhook requests are comming from Telegram.
telegram.bot.webhook.secret=jfoaodscon2h934ddsfwoifj3498f3epmOPAHDFH3QHODLNCS\
        ANCQUIQksir23uqudjspji34jU4RUjfh93 (this is an example, please change it before using)

# Address of the current tomcat server.
telegram.bot.webhook.url=https://example.com

# Required only if there is no DNS.
telegram.bot.webhook.ip=

# The amount of simultaneous connections that is allowed.
telegram.bot.webhook.max_connections=40

# Whether use of custom certificate is necessary. Needed if there is no DNS.
telegram.bot.webhook.use_certificate=false
```

- **Secret key** must be unique for your case. Provide a random `string` there.
- **Address** must be of your current server. If you are using *self-signed certificates*, you need to put your ip and port here.
- **IP** is necessary only in cases where you do not use DNS and a full SSL certificate. DNS without SSL will not work. If you have that, you may skip this field entirely.
- **Max connections** is an advanced setting. I recommend leaving it as the default `40`.
- **Custom certificate** is needed to be `true` if you are using a *self-signed certificate*. This is not recommended in a production setting. Please refer to Telegram's [documentation](https://core.telegram.org/bots/self-signed) on the issue.

**Invoice images**
```properties
telegram.bot.invoice.images.path=invoice/images
```

This property specifies the folder where invoice images for courses are located. I recommend leaving it as it is. Images can be uploaded by the *Director* through the **Control Bot**. Images must be in `.jpg` format.

**Certificates**
```properties
# SSL certificate paths. You have to place your public and private keys in there.
server.ssl.certificate=classpath:/fullchain.pem
server.ssl.certificate-private-key=classpath:/privkey.pem
```

These properties define where the certificate files are located. By default, it is `src\main\resources`. The certificate files must be named exactly as you name them here.

## How to launch the app?
There are **several options** depending on what you want to do. I'm not going to explain this in *extreme detail*, because the general idea is the same as with any other **Spring** application. 

### Testing, local setup
You can use Docker with both of these options (docker-compose), by the way. But be aware that that might complicate things.

**With ngrok**
If you just want to **test** things and see how everything works, by far the **easiest** option is [*ngrok*](https://ngrok.com/). You can reroute *Telegram's requests* through it, therefore avoiding opening ports and setting up a self-signed certificate. *I recommend this option for testing*. How to set it up:
1. Launch **ngrok** and get a **URL** by using something like this: `ngrok http 8080`.
2. Paste that **URL** in `application.properties`'s property `telegram.bot.webhook.url`. Also, make sure `telegram.bot.webhook.use_certificate` is **false**, because you don't need a self-signed certificate here. When there are no certificates, I recommend **removing** these two fields:
```properties
server.ssl.certificate=classpath:/fullchain.pem
server.ssl.certificate-private-key=classpath:/privkey.pem
```
Add a new (or edit) `server.port` property, setting it to `8080` (http).

3. Launch your database. 
4. If you set *all of the other properties* **correctly**, you are good to go. Be aware that **ngrok** changes the **URL** *every time* you launch it, and it lasts for a limited time. So *this is not a deployment solution*, but *perfect* for **testing** nonetheless.

**With a self-signed certificate**
This is a more complicated solution (and the one I used during development). It's harder to explain what you have to do to make it work, since a lot depends on your network setup. Generally, the idea is this:
1. Get a **self-signed certificate**. You can read about it [here](https://core.telegram.org/bots/self-signed).
2. Place the **certificate files** in `src\main\resources` and fill in the properties I *mentioned above*.
3. Set the port to **443** (https).
4. Set `telegram.bot.webhook.use_certificate` to **true** ***(important!)***.
5. Set `telegram.bot.webhook.ip` to the **external IP** of your server.
6. Launch your database.
7. Open the port **443** in your network settings. I can't explain how to do that, because it depends on a lot of factors and can be **quite tricky**. You can check the ports online, for example, [here](https://2ip.io/check-port/). Server **has to be launched**.

If you are **struggling** to identify a **problem**, try going to `https://hostname/webhook/info?botName=internal_bot_name`, where `internal_bot_name` is the name of the **bot** you're testing. There you will be able to see whether **Telegram** is sending any *errors*, and that may help find the **problem**.

### Deployment
I will keep this particular section short, since **deployment** is *difficult* and depends a lot on what *your situation* is. It is *recommended* to use **Docker**. You will also need a legitimate *SSL certificate*. I advise you to **avoid** using **self-signed certificates** in a **production enviroment**.

Due to how I test things, **Docker** has been disabled in `pom.xml`. You will need to *enable* it by uncommenting this area:

```xml
  </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-log4j2</artifactId>
        </dependency>
        <!-- <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-docker-compose</artifactId>        This dependency needs to be enabled.
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency> -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-b
```
Once that's done, you will need to rebuild the app and create a **Docker image**. Use `mvn clean package` to build the project with **Maven**. You will also need to configure the `.env` file. For further info on how to create **Docker images** and *deploy applications*, please refer to *third-party documentation*.

Of course, you can deploy the app without **Docker**, but, again, things depend a lot on **your situation**. For example, *Amazon AWS* works well with **Docker**, but there might be some other providers that do not depend on it so much.
