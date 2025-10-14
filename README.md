## Table of contents
- [Introduction](#introduction)
- [Features](#features)
- [Technologies](#technologies)
- [Organisation](#organisation)
  - [Different user roles:](#different-user-roles)
  - [Types of Bots:](#types-of-bots)
- [Docs](#docs)
- [Future plans](#future-plans)
  - [Fix localizations](#fix-localizations)
  - [Add payment provider support](#add-payment-provider-support)
  - [Quizes](#quizes)
  - [Improve statistics](#improve-statistics)
  - [Other ideas](#other-ideas)
- [Additionally](#additionally)
  - [Licence](#licence)
  - [Other projects](#other-projects)

# Introduction

A **Spring Boot**–based platform for creating and managing small online courses inside **Telegram Bots**, with **role-based access**, **payments** via **Telegram Stars**, **localization**, and *detailed content management*.

Large online learning platforms are often challenging to set up and prohibitively expensive for courses containing **fewer than 30-40 lessons**. This platform addresses these scenarios by providing an efficient and accessible solution for delivering courses via Telegram Bots.

<figure style="display: inline-block; width: 100%; text-align: center; margin: 0 10px">
    <img src="/images/gifs/creating_new_course.gif" alt="Creating new course" width="100%">
    <figcaption><em>Creating new course</em><br /></figcaption>
</figure>

# Features
For a **full** and **detailed** list of *features* of the platform, go [here](/docs/FEATURES.md). Here are the main ones:

 - Different creators have their **own dedicated bots** where their content is located.
 - Application wide **role system**, allowing creators to manage their content, customers and so on.
 - Creators can **add or edit** courses inside the bot:
   <table>
     <tr>
       <td align="center">
         <img src="/images/gifs/course_price_change.gif" alt="Changing course price" width="100%">
         <em>Changing course price</em>
       </td>
       <td align="center">
         <img src="/images/gifs/add_content.gif" alt="Adding content to a course" width="100%">
         <em>Adding new content to a course</em>
       </td>
     </tr>
   </table>
 - Courses have an *optional* **homework system** with automatic or manual approval:
    <table>
     <tr>
       <td align="center">
         <img src="/images/gifs/sending_homework.gif" alt="Sending homework" width="100%">
         <em>Sending homework</em>
       </td>
       <td align="center">
         <img src="/images/gifs/accepting_homework.gif" alt="Accepting homework" width="100%">
         <em>Accepting homework</em>
       </td>
     </tr>
   </table>
 - Courses can be bought with **Telegram Stars** directly in the bot: ![*Buying a course*](/images/gifs/course_purchase.gif)
   - All courses are saved in perpetuity and can be accessed by users again anytime.
   - Courses can also be gifted: ![*Gifting a course*](/images/gifs/give_course.gif)
 - Statistics on the bot's performance can be requested by creators: ![*Statistics*](/images/gifs/statistics.gif)
 - Customers can leave **reviews** when they complete a course: ![*Leaving a review*](/images/gifs/leave_reviews.gif)
 - Customers can request technical or course support: ![*Request support*](/images/gifs/support.gif)
 - Custom **localization system** to support different languages: ![*Select language*](/images/gifs/changing_language.gif)
 - The system works with webhooks and supports self-signed certificates as well as the normal SSL.
 - There are many other small things which are too numerous to mention here.

# Technologies
The project is built using mainly **Spring Framework**. Here is a list of technologies employed:

 - Spring Boot
 - Spring Boot Web
 - Spring Data JPA
 - Lombok
 - Log4j2
 - MySQL 
 - Liquibase
 - Telegram Bots API
 - Telegram Bots SDK
 - Docker

# Organisation

## Different user roles:
- **Director** — has **absolute authority** to make any changes to the **platform**. There can only be one *Director* at a time, and the user that has this role is determined by `application.properties`. This role is mostly needed to make sure that everything works correctly and also to create new **Creator bots**.
- **Creator** — the admin of a *Creator bot* who has **absolute authority** in their **own bot**. The *Creator* is determined by the *Director* when they register a new bot.
- **Support** — can answer **support requests** from users.
- **Mentor** — can resolve **homework requests**.
- **User** — default role for all users.
- **Banned** — a special role that indicates that the user is **banned** and has no access to **this bot**.

## Types of Bots:
- **Control** bot can create new bots for different creators. It is added automatically when the application is initialized for the first time. Only the user with the *Director* role can access this bot.
- **Creator** bot is the main type of bot. Customers interact with these bots to access courses. Every creator bot has a user with the *Creator* role who controls it (can add content, read reviews, give roles, etc.).

# Docs 
Explaining **everything** there is on the **platform** would take *too much* space here, so I've separated that into **separate files**.

- [*Interface*](/docs/INTERFACE.md)
- [*Features*](/docs/FEATURES.md)
- [*How to set thing up?*](/docs/SETUP.md)

# Future plans
Like I've mentioned previously, there are some areas that need **improvement**. Also, I have some ideas for **additional** features that could be added.

## Fix localizations
This is by far the **biggest** problem that needs a *significant* **revamp**. Due to the scale of the changes required, I haven't done it yet, but I **definetely** will in the **future**.

*To put it simply*, **courses** will have the **same system** for *text* as they have for the *images*. The same goes for things like *bot descriptions*, *invoices*, etc.

## Add payment provider support
This will make it possible to pay for courses directly through a **payment provider** avoiding **Telegram Stars** and their numerous issues.

Implementation is relatively straightforward; I just need to dedicate some time to it.

## Quizes
A *potential* future feature for **homeworks**. It will be possible for creators to add **quizes** to **homeworks** to *improve* the *quality of lessons*. I'm not currently thinking about implementing this, but I might in the future when the two major issues explained above are **resolved**.

## Improve statistics
Also, a potential future improvement. Currently, statistics can be useful, but it's relatively limited. It would be cool to add features like performace during a period or even some graphics, like graphs, through external services.

## Other ideas
I have *some other ideas*, but the ones I mentioned *above* are the **main ones**. If you have something to **propose** as well, you can do so here on the [issues](https://github.com/ItsUnbidden/jv-telegram-course-bot/issues) page.

# Additionally

## Licence
This project is licensed under the [MIT License](/LICENSE).

## Other projects
**ReadHub**, backend for an online book store: [link](https://github.com/ItsUnbidden/jv-book-store).

**Task Management System**, backend for a project management platform: [link](https://github.com/ItsUnbidden/jv-task-management-system).
