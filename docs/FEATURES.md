# Features
Here I'll explain how different **subsystems** of the platform work.

- [Features](#features)
  - [Content](#content)
    - [Mappings](#mappings)
  - [Course](#course)
    - [Lesson](#lesson)
    - [Homework](#homework)
  - [Purchases](#purchases)
    - [Refund](#refund)
  - [Reviews](#reviews)
  - [Support](#support)
  - [Statistics](#statistics)

## Content
**Content** represents one **Telegram message**. It can be *video(s)*, *image(s)*, *audio(s)*, *document(s)*, or just *text*. **Content** is used for communication between the bot and the *user*. Internally, videos and images are grouped under *Graphics* due to how **media groups** work. *Images* and *videos* can be combined in one **media group**. Telegram limits the size of a **media group** to *10*, so that is the maximum size for the platform as well. 

When you are asked to supply *user content* (like for a **homework response** or a **review**), you usually just need to send the *message* and **confirm it** in the menu that appears. There might be some *specific requirements* for some cases, which need to be **clearly** explained in the *request messages*.

*Sending content:*

![Sending content](/images/content_upload.png)

### Mappings
Because the platform is localized, content for lessons is grouped in **mappings**. One **mapping** may contain *several contents* where each content is bound to a **language code**. **Mapping** also has a *position* to clearly define the order in which the *lesson* will be sent. 

For **mappings**, sending content is a *little different*. After you've sent the *main message*, you need to send *another one* with the **language code** for this content. This language code will be used to localize the content. **Don't mind** that your message will be *after the confirmation button* — it doesn't matter. Of course, you'll have to press the button **after** you've sent both messages.

*Sending new content for a lesson:*

![Sending new content for a lesson](/images/mapping_content_upload.png)

Some things need to be clarified here. Since **mappings** only represent internal content that relies on *localization files*, you **cannot** send *text content* through the bot. Text is automatically loaded for **lessons** and **homeworks** from *localization files*. If you want to *disable* text, you can do so using `/content` -> **Get Mapping** -> **Toggle Text**. This system has *limitations* due to the project’s **early design**, but will be **improved** in future versions.

## Course
**Courses** represent the **main purpose** of the *platform*. They consist of **lessons** which have a certain order in a **course**. When a **course** is launched, the user doesn't immediately see all the **lessons**, but only the *first one*. To progress, they have *severel options* that depend on the **course** *settings*:
- Press **continue** button.
- Complete **homework**
- Wait

*More about each option:*
If the **lesson** does not have a **homework** (or it is disabled) and there is no **delay**, the **continue** button will appear. If the user presses it, the next lesson appears. 

If the **lesson** has a **homework**, then the only way to progress forward is to **complete** it. If there is no **delay**, the moment it is approved, the user will progress to the next lesson.

If there is a **delay**, the **lesson** will be sent after a *certain amount of time*. Like I've already mentioned above, even if the **homework** is present, the **delay** will still work.

The **course** is considered **completed** after the *last lesson* is sent or its **homework** is approved. When that happens, a *special message* will be sent as well, along with the *last lesson*. Also, a **review** will be requested from the user. 

After the course is completed, the user can select any lesson through `/courses` -> Select course -> **Select lesson**.
### Lesson
A **lesson** represents one step of a **course**. It consists of content, either one or many. As explained in the *[Content](#content)* section, **lessons** have a special *localized content* system which uses **mappings** and also needs text to be written in localization files.

*Localization mapping* for **lesson** content has this pattern: `<courseName>_lesson_<lessonIndex>_content_<contentIndex>`.
- `<courseName>` — internal name of the **course**
- `<lessonIndex>` — a number representing the **position** of this **lesson** in the **course**. The first one is *0*.
- `<contentIndex>` — a number representing the **position** of a specific **content** in a **lesson**. The first one is also *0*.

This localization must be written in the `course` file.

### Homework
**Homework** system allows the *Creator* to partially guide *users* through **courses** by giving **feedback** and therefore improving overall **experience**. It is *optional* and **courses** can be made without a single **homework**. A default homework pipeline looks like this: 
- *User* receives **homework** after a **lesson**.
- *User* sends the **homework** content.
- The *Creator* (and *Mentors* if present) receive a **homework request**.
- One of them *approves* or *declines* the **request**.
  - If they *decline*, user has to send the **homework** again. 
  - If they *approve*, user **advances** to te next **lesson**.

There is also an option to *disable* **feedback**. If this is *enabled*, the **homework** will be *approved* **automatically** after the *user* sends it. The *Creator* will still receive the content, but *no approval option will be presented*.

A **homework** can also have a **delay**, just like with **lessons**. If delay is *enabled*, the **homework** will not be immediately sent after the **lesson**.

**Homework** can also be *disabled* either for a specific **lesson** or for the entire **course**. The *Director*, the *Creator*, and the *Mentors* can toggle whether they will receive **homework requests**. If there are *no users* in a bot who can receive **homework requests**, it will be practically *disabled* for the **entire bot**.

Just like **lessons**, **homework** uses *localized content* with **mappings**, and needs its text to be written in localization files. *Unlike* **lessons**, only one **mapping** can be assigned to a **homework**.

There is also an option to **restrict** what **types of media** a user can send as a **homework**. *For example*, you can configure things in such a way that only **audios** and **text** are accepted.

*Sending an incorrect media type as a homework:*

![Sending incorrect media type](/images/gifs/sending_homework_incorrect_mediatype.gif)

*Localization mapping* for **homework** content has this pattern: `<courseName>_lesson_<lessonIndex>_homework_content`.
- `<courseName>` — internal name of the **course**
- `<lessonIndex>` — a number representing the **position** of this **homework's lesson** in the **course**. The first one is *0*.

This localization must be written in the `course` file.

## Purchases
The **platform** supports internal *Telegram payments* using **Telegram Stars**, so *users* can pay for **courses** directly in their respective bots. Here is how it works:
- A *User* opens the `/courses` menu and chooses a **course** they don't own. It's also possible that they use a *special link* to the **course**.
- Bot sends an **invoice**.
- The *user* uses the **invoice** to buy the **course** with **Telegram Stars**. They can buy **Stars** right there, if there is not enough.
- The **course** is added to the *user*'s *library* and is automatically started.

No specific setup is necessary — everything works by default.

If the *Creator* wants to **give a course** to someone without *Telegram payments*, they can do that in this way: `/coursesettings` -> **Give/take course** -> *Give course (choose user)*. They can also **take a course** from a *user* using the same menu. Be aware that courses that have been **bought** *cannot be taken away*.

**Invoices** can contain a **product image** (*recommended to always include one*). Only the *Director* can upload **invoice images** due to how they work. Also, the **invoice images** will only be able to load if the server has a legitimate address that uses DNS and has an SSL certificate.

In the future, **direct payments** with **payment providers** might be implemented to avoid **Stars** and their issues.

### Refund
There is an *optional* function to include a possibility of a **refund** for a **course**. *Creators* can enable it using `/coursesettings`. Whether the *user* can **refund** a **course** depends on what **lesson** the *user* is at. If they **surpassed** the **lesson** specified by the *Creator* in the **course settings** (*current_stage* > *refund_stage*), **refund** will be *blocked*. If the **course** has already been **completed**, a **refund** is *impossible*. **Refund** will also be *disabled* if more than **21 days** has passed since the **course** was bought. This last condition is due to how **Telegram Stars** work.

*Users* can access **refund** here: `/courses` -> *Select course* -> **Refund**. This button will *not be present* at all if the refund is unavailable for some reason.

*Refunding a course:*

![Refund example](/images/gifs/course_refund.gif)

## Reviews
**Reviews** are a way for the *Creators* to receive feedback on their **courses** from *users* and improve them. Currently, **reviews** cannot be seen by *users*, only the *Director*, the *Creator*, and *Mentors*.

A **review** can only be given if the *user* has completed a **course**. In fact, a *proposition* to leave a **review** will **automatically** be presented after *finishing*. There are *two types* of **reviews**:
- Basic
- Advanced

**Basic review** is a simple **quiz** with just *two questions*: "*How would you rate this course from 1 to 10?*" and "*How would you rate this platform from 1 to 10?*". To simplify everything even further, the user does not need to write anything. Instead, they just press a button with the mark of their choice.

*Basic review first step:*

![Basic review](/images/review_basic.png)

When the *user* answers these **two questions**, they have left a **basic review**. After that, another *proposition* to leave an **advanced review** will be given. **Advanced review** allows the user to send content as a **review**, so that might be basically *any media*. That will be added as an attachment to the **basic review**.

*Advanced review request:*

![Advanced review](/images/review_advanced.png)

*Creators* (and *Mentors*) can view reviews using `/reviews`. There, they can choose to see **new** or **archived reviews**. 
- **New reviews** will be shown in batches (3 by default). *Creators* can **mark them as read**, or **send a comment**. 
  - **Mark as read** moves the review into the archive.
  - **Comment** allows the *Creator* to send some **content** to the *user*. This will automatically **mark the review** as archived.
- **Archived reviews** will be compiled into a **.txt file** and then sent to you. **Content** will obviously not be included there, but you can use *IDs* to find it in the bot by using `/content`. 

*Users* can edit their **reviews** using `/courses` and choosing the **course** there. Both **basic** and **advanced reviews** can be edited. If a **review** gets edited, it will be *marked as **new** again*.

**Comments** for **reviews** can also be edited if a **review** is *modified*, although only by the one *who left* the **comment** in the first place.

## Support
**Support** allows *users* to request **technical** or **course support** from *admins (the Director, Creators, Support)*. To access support, the user needs to use `/support`. Support request contains content (any type of media). When *someone* requests **support**, the request is sent to all *Support* users. A **course support request** is also sent to the *Creator*, while a **technical support request** is sent to the *Director*.

When a **support request** is received, it needs to be answered with a **reply**. After *at least one* **reply**, the request can be **resolved** by either side.

*A support request:*

![Support request](/images/support_request.png)

*A support reply:*

![Support reply](/images/support_reply.png)

## Statistics
**Statistics** is a simple feature that allows **monitoring** of some *important values* in a bot. It can be accessed with `/statistics`.
There are three options available:
- Bot statistics
- Course statistics
- Registered users

*Bot statistics:*

![Bot statistics](/images/statistics_bot.png)

**Course statistics** allows you to see general **statistics** or *users* who have this **course**. 

*Course statistics:*

![Course statistics](/images/statistics_course.png)

*Users* can be filtered by either **course stage** or **course completion**. *All users* can also be requested. If there are too many *users*, a menu with *several pages* will appear.
