# INCHAT – The INsecure CHAT application

Welcome to this second mandatory assignment of INF226.
In this assignment you will be analysing the security
of a program called inChat – a very simple chat application,
in the shape of a [Jetty](https://www.eclipse.org/jetty/)
web application.

inChat has been especially crafted to contain a number
of security flaws. You can imagine that it has been
programmed by a less competent collegue, and that after
numerous securiy incidents, your organisation has decided
that you – a competent security professional – should take
some time to secure the app.

## Getting and building the project

Log into [`git.app.uib.no`](https://git.app.uib.no/Hakon.Gylterud/inf226-2020-inchat) and make your
own fork of the project there. *Make sure your fork is private!*
You can then clone your repo to your own machine.

To build the project you can use Maven on the command line, or configure
your IDE to use Maven to build the project.

 - `mvn compile` builds the project
 - `mvn test` runs the tests. (There are only a few unit test – feel free to add more).
 - `mvn exec:java` runs the web app.

Once the web-app is running, you can access it on [`localhost:8080`](http://localhost:8080/).

## Updates

Most likely the source code of the project will be updated by Håkon
while you are working on it. Therefore, it will be part of
your assignment to merge any new commits into your own branch.

## Improvements?

Have you found a non-security related bug?
Feel free to open an issue on the project GitLab page.
The best way is to make a separate `git branch` for these
changes, which do not contain your sulutions.

(This is ofcourse completely volountary – and not a graded
part of the assignment)

If you want to add your own features to the chat app - feel free
to do so! If you want to share them, contact Håkon and we can
incorporate them into the main repo.


