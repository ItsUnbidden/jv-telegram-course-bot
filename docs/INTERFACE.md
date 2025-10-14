# Interface
Here I'll explain the **interface** of the platform. Generally, interraction with bots is conducted through a combination of **commands** and **menus**.
- **Commands** are needed to call a *specific menu*.
- User navigates through the **menu** to access *features*.

Available **commands** can be found using the `Menu` button. Only commands that user can actually access are seen there. For example, user will not see the `/admin` command.

Here is a **detailed list** of available **commands and menus** categorized by user roles:

- [Interface](#interface)
  - [General commands](#general-commands)
  - [User](#user)
  - [Creator](#creator)
  - [Director](#director)
    - [Control Bot](#control-bot)
    - [Creator bot](#creator-bot)
  - [Auxillary roles](#auxillary-roles)
    - [Support](#support)
    - [Mentor](#mentor)

## General commands
These commands are available to **all roles**:

`/language` — opens the **language** menu. Allows the user to manually choose a language. Be aware that the available languages are pulled from the language priority property in `application.properties`.
`/creator` — shows info about the *Creator*. This is a simple text message that is set up through localization files.
`/start` — shows intial *start message* (works the same as `/creator`) and registers the user on the platform.
`/terms` — shows *terms and conditions*. Works the same as `/creator`.

## User
**Available commands:**
`/courses` — opens the **courses** menu. This menu allows the user to choose a course. It is dynamic and calls other menus depending on the situation:
- If the user owns **no courses** — shows available courses.
- If the user owns **all available courses** — shows owned courses.
- If the user owns **some courses** — the user can choose what kind of courses they want to see.

If the user chooses a course they **don't have**, an **invoice** for that course will be sent.

If the user chooses a course they own, the result depends:
- If the course was not completed — the last lesson the user was on opens.
- If the course was completed — additional options are available:
  - **Start course** — start the course from the first lesson.
  - **Select stage** — the user can select the lesson they want to start from.
  - **Refund** — starts course *refund procedure*. This option is only available if the conditions are satisfied. I cover this in more detail [here](/docs/FEATURES.md/#refund).
  - *Some type of review setting* — what kind of setting this will be *depends on what kind of review* the user left when they completed the course, if any. Reviews are covered in detail later [here](/docs/FEATURES.md/#reviews).

How courses work in general I explain [here](/docs/FEATURES.md/#course).

`/support` — opens the support menu. Allows the user to request support from the *Director*, *Creator*, or *Support* roles. I explain support feature in more detail [here](/docs/FEATURES.md/#support).

## Creator
Has **absolute authority** in their **own bot**. Represents the content creator. This role can create new courses, add and modify content, answer homework requests, answer support requests, etc.

**Available commands:**
`/admin` — opens the **admin menu**. Here, the *Creator* can set user roles, ban users, toggle receiving homework, and list current users with special roles.
- **Set role for user**
  - **Select role** — select the role you want to give.
    - **Select user** — select user from your contacts list.

A *notification* will be sent to you and the person you gave the role to.
- **Admin list** — lists **all users** with *special roles* in this bot.
- **Toggle receive homework** — *enables/disables* the function of **receiving homework**.
- **Bans** — bans a user or lifts a previous ban in this bot.
  - **Give ban**
    - **Select user** — select a user from your contacts list or by ID.
      - Send the **number of hours** the user will be banned for (from 0 to 720). 0 or less means forever.
  - **Lift ban**
    - **Select user** — select a user from your contacts list or by ID.

`/content` — an advanced command that allows some content fine-tuning.
- **Upload content**
  - Send content. This is a debug feature that will be removed later. Only the *Director* can use this feature.
- **Get content**
  - Send content id to load content. You can't load content from another bot. 
- **Get mapping** — this is a useful function since it allows setting up differently localized versions of content.
  - Send mapping id.
    - **Add localization**
      - Send new content. More about content I explain *[here](/docs/FEATURES.md/#content)*.
    - **Remove localization**
      - Send language code of the localization you want to remove.
    - **Toggle text** — enables/disables text for this localization.

`/coursesettings` — this is one of the main menus on the platform and one of the most complex. It allows creation, modification, and deletion of courses. More about courses you can read *[here](/docs/FEATURES.md/#course)*.
- Select a course
  - **Change price**
    - Send the new price. It must be higher than 0⭐️ and lower than 100000⭐️. 
  - **Give or take course**
    - **Give** 
      - **Select user** — select user from your contacts list.
    - **Take**
      - **Select user** — select user from your contacts list. You can't take a course from a person who bought it.
  - **Toggle homework feedback** — enables/disables homework feedback for this course. For more info on **homework** [click here](/docs/FEATURES.md/#homework).
  - **Toggle homework** — enables/disables homework for this course.
  - **Toggle maintenance** — enables/disables maintenance for this course. If enabled, no user can interract with this course. 
  - **Refund stage** — specifies a lesson after which refund is impossible.
    - Send lesson index. Negative number means refund is not allowed.
  - **Lessons** — opens lessons selection.
    - Select a lesson
      - **Add content** — creates a new mapping for content. If you want to add localizations for content, check `/content`.
        - Send new content. More about content I explain *[here](/docs/FEATURES.md/#content)*.
      - **Remove content** — removes a mapping.
        - Send mapping ID. Remember, the name here might be a little misleading: you're removing a **mapping**, not just *one content* (*in case there are more than one in this *mapping).
      - **Change order** — changes mapping order in this lesson.
        - Send the ID of the mapping you want to move.
        - Send the index where you want it to be.
      - **Set delay**
        - Send the new lesson delay. *0* means there is no delay. Max value is *4320 minutes*.
      - **Delete lesson** — deletes this lesson. You have to confirm this by sending the course's internal name.
      - One of two options will be available depending on whether you have homework for this lesson:
        - **Create homework**
          - Send new content for this homework.
        - **Homework settings**
          - **Update content** — replaces the current mapping with a new one.
            - Send the new content.
          - **Media types** — what media types are allowed as a response to this homework.
            - Send the allowed types. They must be written like this: `TEXT GRAPHICS`. Available options: 
              - `GRAPHICS` — photos and videos.
              - `AUDIO` — audio files.
              - `DOCUMENTS` — any file.
              - `TEXT` — text message.
              - `null` — any content.
          - **Set delay**
            - Send the new lesson delay. *0* means there is no delay. Max value is *720 minutes*.
          - **Toggle feedback** — enables/disables feedback for this particular homework. Ignored if homework is disabled for this course.
          - **Toggle repeated completion** — enables/disables repeated completion of this homework. I recommend keeping this **turned off**.
    - **Add new lesson**
      - Send the position of the new lesson. *First is 0*.
  - **Delete course** — deletes this course. You have to confirm this by sending the course's internal name. Initial course cannot be deleted.
    - Send course's internal name.

`/localization` — this is a **debug command** available only to the *Director*. It allows loading a **specific localization**. You need to include two paramaters:
- Localization name
- Language code

Be aware, that this particular command is **unique** in how the *parametes* are supplied. You need to enter this command **like this**: `/localization localization_name lanugage_code`. It's unlikely you will need it, unless you want to see how particular localization looks in Telegram.

`/post` — opens the **post** menu. Here you can send a message to a certain group of users in this bot. For example, you can send a message to all *Mentors*. Be aware that this feature is quite heavy on performance and only one `Post` operation can be conducted at one time due to this issue.
- **List of roles** — choose a role, users of which will receive your message.
  - Send your message.
- **Custom role set** — define a custom role set.
  - **Send the role set** — role names must be separated by `whitespace`s.
    - Send the message

`/reviews` — opens the **reviews** menu. Here you can view and answer users' reviews. For more info on reviews, *[click here](/docs/FEATURES.md/#reviews)*.
- Select course — choose a **specific course** or **all courses**.
  - **New reviews** — shows three of the newest reviews to you. You have two or three options for each:
    - Mark as read — marks this comment as archived.
    - Get comment — shows the comment.
    - Answer/Update comment — allows to send content for a new comment. Update is possible only if you are the one who sent it in the first place.
      - Send content.

  - **Archive reviews** — compiles a text file with all availble reviews for this course (or all courses). Content is not included in this file and can be viewed with `/content` command using IDs.

`/statistics` — opens the **statistics menu**. This menu allows requesting some statistics data on the bot's performace. More on statistics you can read *[here](/docs/FEATURES.md/#statistics)*.
- *Select course* — Choose a course to show statistics for.
- **Bot statistics** — shows this bot's statistics.
- **Bot users**
  - **By stage**
    - *Choose stage* — shows all users on a particular stage (lesson).
  - **Completed course** — shows all users who completed this course.
  - **All users** — shows all users.

## Director
This role can **access everything** across the **entire platform**. Its main functions are to set up new bots, localization files, invoice images, and resolve technical issues. 

### Control Bot
**Available commands:**
`/maintenance` — allows enabling/disabling maintenance. During maintenance no requests to the server can be made. This is useful to ensure nothing is interrupted during server shutdown.

`/refresh` — opens the **refresh** debug menu.
- **Refresh localizations** — reloads localizations files.
- **Refresh bot names and descriptions** — resets bot names and descriptions from new localizations. Does not automatically reload localization files.
- **Refresh menus** — reload Telegram command menus. Does not automatically reload localization files.

`/generalban` — opens the **general ban** menu. This type of ban prevents the user from interracting with the platform **completely**. Works largely the same as a bot-wide ban. Check *[Creator](#creator)* if you want more info.

`/botsettings` — opens a **bot settings** menu. It allows adding new bots or listing currently registered bots.
- **Create bot**
  - **Select user** — select user from your contacts list or by ID.
    - Send two messages: 
      1. Bot name — this name is internal, so it's not as important.
      2. Bot token — this must be provided by the creator after they create a Telegram Bot.
- **List bots** — lists all registered bots along with their creators.

`/generalpost` — opens a **general post** menu. Here you can send a message to a certain group of users in all bots. For example, you can send a message to all creators. Works largely the same as it's bot-wide equivalent. Check *[Creator](#creator)* if you want more info.

`/files` — opens a **files** menu. Here you can upload new **localization files** and **invoice images**. You can also delete invoice images if necessary.
- **Localization files**
  - Send localization files. Be aware that they **must** follow the localization files pattern that I explained in [What about localizations?](/docs/SETUP.md/#what-about-localizations). Also, the last message **must be the language code**.
- **Invoice image**
  - Send the invoice image. It must be in the `.jpg` format. The last message must be the **internal course name**.
- **Delete image**
  - Send the course name.

### Creator bot
Holds the same authority as the local **Creator** and can access **everything**. Cannot be banned by the *Creator*.

## Auxillary roles
These roles are **not necessary** to be present in a bot. They are mostly needed in cases where the creator wants to delegate responsibility for some things to other people. In many bots, these roles *will not be needed at all*.

**Commands** for these roles are watered-down versions of the *Creator* role's commands. So, if you want to know more, go to *[Creator](#creator)*

### Support
This role is mostly needed to answer **support requests**. Can ban users. Cannot access normal user commands. For more info on **support** [click here](/docs/FEATURES.md/#support).
**Available commands:**
`/admin` — opens the *admin* menu. Only one option is available to this role here: **ban users**.
`/statistics` — opens the **statistics** menu.

### Mentor
This role is mostly needed to help the *Creator* in answering **homework requests**. Can ban users and post messages. Cannot access normal user commands. For more info on **homework** [click here](/docs/FEATURES.md/#homework).

**Available commands:**
`/admin` — opens the *admin* menu. Two options are available to this role here: **ban users** and **Toggle receive homework**.
`/statistics` — opens the **statistics** menu.
`/review` — opens the **review** menu.
`/post` — opens the **post** menu.
