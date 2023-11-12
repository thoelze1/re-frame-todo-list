# re-frame-todo-list

A [re-frame](https://github.com/day8/re-frame) to-do list manager.

## To Do

- Use re-frame's http-fx library (wrapper for cljs-ajax)
- Implement routing
  - I can see that using `defmulti` as in
    [crux-db-front-end](https://github.com/danownsthisspace/crux-db-front-end)
    is an abuse of clojurescript's (lack of) arity checking. I like
    the encapsulation (`srticles.views` just makes a call to
    `articles.route/panels`) but I think the interface for adding
    panels should just use another mechanism (instead of `defmulti`).
- Follow https://www.youtube.com/watch?v=6jvG3XbSeos to add a single
  server in this repo that will serve both the frontend and backend.

## Notes

To use namespaces in a CLJS repl, you must `require` them

## Quickstart

```
$ npm install
$ npm run watch
```
## `shadow-cljs` dependencies

By default, `shadow-cljs` can access npm packages directly. This is preferred, but in case you require a CLJS library that itself uses CLJSJS packages, shadow-cljs just needs a shim file. The `shadow-cljsjs` library provides many of these shims.

If `:require`s aren't working, try

```
$ shadow-cljs browser-repl
```

or

```
$ shadow-cljs node-repl
```

## Routing

`react-router` has over 50k stars on GitHub so I gave it a try. But supposedly "routing has nothing to do with dendering the view". Routers do two different things: 1) offer a bijection between route-as-value and route-as-html-url-string and 2) integrate with web browser history apis and web framework http request parsing. I guess react-router is coupling routing with rendering the view:
https://www.reddit.com/r/Clojure/comments/a6lokt/routing_with_reframereagent/

I want to just try reitit

Wow, is routing just an artifact from browser navigation UIs and search engine indexing? 

https://stackoverflow.com/questions/39636411/what-is-routing-why-is-routing-needed-in-single-page-web-apps

"The fact that the appearance of the app may change between states is incidental to what is really going on. You should only implement a route in an SPA where there is a state of the app which you want the user to be able to return to."

So really, I definitely don't need routing yet.

Update 7/11/2023: as shadow-cljs hot reloads my code, it's getting annoying to renavigate to the state I was wanting to currently test. I'm operating in the dark, and since reitit has a lot more usage than cljs-ajax, I'm gonna give it a try.

## HTTP Servers

Supposedly the asynchronous stuff in aleph is first class and it exposes data from the network as a manifold stream, which makes it play well with websockets. A possible alternative to jetty.

## Structuring backend and frontend

A web application can suse a different server for each of its services: the frontend, the backend, the database, etc. If we're talking about just frontend and backend, let's think of how code can be structured:

        1 Server    2 Servers
 
1 repo  Works       But why?

2 repos Nope        Works



## Clojure resources
https://www.clojure-toolbox.com/

## Front-end and back-end... what is a website?

"What is the difference between opening an html file using localhost vs `file://`?" Short answer: browsers are complicated and treat local files and files served from webservers (including localhost) differently.
https://stackoverflow.com/questions/40204913/difference-between-localhost-and-opening-html-file

Even an SPA written in vanilla javscript is served from webserver: https://dev.to/dcodeyt/building-a-single-page-app-without-frameworks-hl9

It's possible to use react, an SPA framework, to build an app that is openable via `file://`, but you would have to configure your bundler (often webpack) to process all of the URLs properly: https://www.reddit.com/r/reactjs/comments/uchls5/can_a_react_app_run_locally_like_an_html_page/

So what is a bundler? What exactly is the output of a "compiled" react app? Well, the JSX gets transpiled to normal js (ideally a form compaitble with a wide array of browsers) by babel, then webpack "bundles" the js and its dependencies into fewer optimized files, possibly employing minification, tree-shaking, etc, while also bundling other assets (css, images, fonts etc) into a final form that is ready to be served to a web browser for execution. 
https://poe.com/s/kHJn9SlVVLdQqi3HEzzy

So the frontend is code that is served to the user's browser by some server, call it "x". Is server "x" the same server that accepts other requests, and that we call the backend? Yes, the front-end is served by the back-end.

So the browser has a javascript engine that it uses to run javascript code as it is encountered in the HTML file or as it is triggered by user interaction

AJAX is supported via native browser APIs such as `fetch()`. There are also libraries that further abstract these native AJAX APIs, such as Axios or jQuery AJAX functions. The important point is that your react app doesn't use AJAX until you need it: for storing persistent, server-side state, e.g.
https://poe.com/s/R1zybxpn8LxDPVtEMyS0

What is jQuery? jQuery is a javascript library that kind of does... everything. It's leff popular now, as many features it provides are now available in modern javascript and browser APIs. Not many modern tools rely on jQuery anymore. Earlier versions of bootstrap used jQuery, but not very many modern tools do.
https://poe.com/s/L456sfJHce4JrsJKBjMp

Before AJAX, how did web development go? Before jQuery, how did web development go? Before AJAX, the entire web page would reload after interaction with the user (submitting a form, e.g.) or at regular intervals. Also, server-side rendering (of the HTML served to the client) was necessary.
https://poe.com/s/ETSfiZewZmI3T83oUX51

"This is the old-school way of getting data for your app. The data is embedded in the HTML sent from the server. If you want fresh data, you need to refresh the page manually or have the page refresh periodically. Remember this?"
https://blog.logrocket.com/comprehensive-guide-data-fetching-react/

So I'm still trying to figure out whether there is a front-end "server". Based on chatgpt's response, the answer is yes: you need a server to serve static assets (more than html/js/css: images, files, etc) both when the browser first visits the page, and on demand if the user wants to open a specific file (you can't have the user download the entire content of the website when first visiting the page).

According to this answer, the server is only necessary during development:
https://stackoverflow.com/questions/60011485/why-does-react-have-a-server-on-its-own

My understanding is that since react does not use AJAX, it uses entirely in-browser APIs to render the front-end and therefore react does not have any code running on the server side. This makes sense based on my experience writing the chess app: I was able to easily deploy it to github pages which is a static site generator! Of course any website's assets need to be served by an HTTP server, which is what github pages was doing with my compiled react. ChatGPT claimed that the front-end server may also need to handle routing and URL resolution (especially in SPAs where "the front-end code dynamically updates the content without full page reloads"). But in a vanilla react app without routing, this isn't true, I don't think. I wonder if github pages would serve a site that requires routing? I believe ChatGPT's answer is more relevant to other frameworks like NextJS that do server-side rendering. If only for serving the static site and even doing some routing, I can now see why you need a server hosting the front-end. So is it an HTTP server?

Of course it's an HTTP server! See: `http://google.com`. HTTP is the protocol that your browser uses, so of course a website that your browser interacts with is going to have to respond on that protocol.

## Single Page Applications (SPAs)

Watch out for how to scale up your app: caching (cloudflare, e.g.), SEO, socials-sharing
https://stackoverflow.blog/2021/12/28/what-i-wish-i-had-known-about-single-page-applications/

## CSS

Note: I have not yet chosed a CSS build system, so certain files are hard copied from their node package to `resources/` (`react-datepicker`, for one). I feel this is a problem best left until deployment.

Importing CSS from `npm` modules is unsupported in `shadow-cljs`:
https://github.com/thheller/shadow-cljs/issues/353

The author recommends using `SASS`.

Interop example: https://github.com/iku000888/reagent-shadow-cljs-vis-js/blob/master/src/my/dev.cljs

## Getting Started

### Project Overview

* Architecture:
[Single Page Application (SPA)](https://en.wikipedia.org/wiki/Single-page_application)
* Languages
  - Front end is [ClojureScript](https://clojurescript.org/) with ([re-frame](https://github.com/day8/re-frame))
* Dependencies
  - UI framework: [re-frame](https://github.com/day8/re-frame)
  ([docs](https://github.com/day8/re-frame/blob/master/docs/README.md),
  [FAQs](https://github.com/day8/re-frame/blob/master/docs/FAQs/README.md)) ->
  [Reagent](https://github.com/reagent-project/reagent) ->
  [React](https://github.com/facebook/react)
* Build tools
  - CLJS compilation, dependency management, REPL, & hot reload: [`shadow-cljs`](https://github.com/thheller/shadow-cljs)
* Development tools
  - Debugging: [CLJS DevTools](https://github.com/binaryage/cljs-devtools)

#### Directory structure

* [`/`](/../../): project config files
* [`dev/`](dev/): source files compiled only with the [dev](#running-the-app) profile
  - [`user.cljs`](dev/cljs/user.cljs): symbols for use during development in the
[ClojureScript REPL](#connecting-to-the-browser-repl-from-a-terminal)
* [`resources/public/`](resources/public/): SPA root directory;
[dev](#running-the-app) / [prod](#production) profile depends on the most recent build
  - [`index.html`](resources/public/index.html): SPA home page
    - Dynamic SPA content rendered in the following `div`:
        ```html
        <div id="app"></div>
        ```
    - Customizable; add headers, footers, links to other scripts and styles, etc.
  - Generated directories and files
    - Created on build with either the [dev](#running-the-app) or [prod](#production) profile
    - `js/compiled/`: compiled CLJS (`shadow-cljs`)
      - Not tracked in source control; see [`.gitignore`](.gitignore)
* [`src/re_frame_todo_list/`](src/re_frame_todo_list/): SPA source files (ClojureScript,
[re-frame](https://github.com/Day8/re-frame))
  - [`core.cljs`](src/re_frame_todo_list/core.cljs): contains the SPA entry point, `init`
* [`.github/workflows/`](.github/workflows/): contains the
[github actions](https://github.com/features/actions) pipelines.
  - [`test.yaml`](.github/workflows/test.yaml): Pipeline for testing.


### Editor/IDE

Use your preferred editor or IDE that supports Clojure/ClojureScript development. See
[Clojure tools](https://clojure.org/community/resources#_clojure_tools) for some popular options.

### Environment Setup

1. Install [JDK 8 or later](https://openjdk.java.net/install/) (Java Development Kit)
2. Install [Node.js](https://nodejs.org/) (JavaScript runtime environment) which should include
   [NPM](https://docs.npmjs.com/cli/npm) or if your Node.js installation does not include NPM also install it.
5. Clone this repo and open a terminal in the `re-frame-todo-list` project root directory

### Browser Setup

Browser caching should be disabled when developer tools are open to prevent interference with
[`shadow-cljs`](https://github.com/thheller/shadow-cljs) hot reloading.

Custom formatters must be enabled in the browser before
[CLJS DevTools](https://github.com/binaryage/cljs-devtools) can display ClojureScript data in the
console in a more readable way.

#### Chrome/Chromium

1. Open [DevTools](https://developers.google.com/web/tools/chrome-devtools/) (Linux/Windows: `F12`
or `Ctrl-Shift-I`; macOS: `⌘-Option-I`)
2. Open DevTools Settings (Linux/Windows: `?` or `F1`; macOS: `?` or `Fn+F1`)
3. Select `Preferences` in the navigation menu on the left, if it is not already selected
4. Under the `Network` heading, enable the `Disable cache (while DevTools is open)` option
5. Under the `Console` heading, enable the `Enable custom formatters` option

#### Firefox

1. Open [Developer Tools](https://developer.mozilla.org/en-US/docs/Tools) (Linux/Windows: `F12` or
`Ctrl-Shift-I`; macOS: `⌘-Option-I`)
2. Open [Developer Tools Settings](https://developer.mozilla.org/en-US/docs/Tools/Settings)
(Linux/macOS/Windows: `F1`)
3. Under the `Advanced settings` heading, enable the `Disable HTTP Cache (when toolbox is open)`
option

Unfortunately, Firefox does not yet support custom formatters in their devtools. For updates, follow
the enhancement request in their bug tracker:
[1262914 - Add support for Custom Formatters in devtools](https://bugzilla.mozilla.org/show_bug.cgi?id=1262914).

## Development

### Running the App

Start a temporary local web server, build the app with the `dev` profile, and serve the app,
browser test runner and karma test runner with hot reload:

```sh
npm install
npx shadow-cljs watch app
```

Please be patient; it may take over 20 seconds to see any output, and over 40 seconds to complete.

When `[:app] Build completed` appears in the output, browse to
[http://localhost:8280/](http://localhost:8280/).

[`shadow-cljs`](https://github.com/thheller/shadow-cljs) will automatically push ClojureScript code
changes to your browser on save. To prevent a few common issues, see
[Hot Reload in ClojureScript: Things to avoid](https://code.thheller.com/blog/shadow-cljs/2019/08/25/hot-reload-in-clojurescript.html#things-to-avoid).

Opening the app in your browser starts a
[ClojureScript browser REPL](https://clojurescript.org/reference/repl#using-the-browser-as-an-evaluation-environment),
to which you may now connect.

#### Connecting to the browser REPL from your editor

See
[Shadow CLJS User's Guide: Editor Integration](https://shadow-cljs.github.io/docs/UsersGuide.html#_editor_integration).
Note that `npm run watch` runs `npx shadow-cljs watch` for you, and that this project's running build ids is
`app`, `browser-test`, `karma-test`, or the keywords `:app`, `:browser-test`, `:karma-test` in a Clojure context.

Alternatively, search the web for info on connecting to a `shadow-cljs` ClojureScript browser REPL
from your editor and configuration.

For example, in Vim / Neovim with `fireplace.vim`
1. Open a `.cljs` file in the project to activate `fireplace.vim`
2. In normal mode, execute the `Piggieback` command with this project's running build id, `:app`:
    ```vim
    :Piggieback :app
    ```

#### Connecting to the browser REPL from a terminal

1. Connect to the `shadow-cljs` nREPL:
    ```sh
    lein repl :connect localhost:8777
    ```
    The REPL prompt, `shadow.user=>`, indicates that is a Clojure REPL, not ClojureScript.

2. In the REPL, switch the session to this project's running build id, `:app`:
    ```clj
    (shadow.cljs.devtools.api/nrepl-select :app)
    ```
    The REPL prompt changes to `cljs.user=>`, indicating that this is now a ClojureScript REPL.
3. See [`user.cljs`](dev/cljs/user.cljs) for symbols that are immediately accessible in the REPL
without needing to `require`.

### Running `shadow-cljs` Actions

See a list of [`shadow-cljs CLI`](https://shadow-cljs.github.io/docs/UsersGuide.html#_command_line)
actions:
```sh
npx shadow-cljs --help
```

Please be patient; it may take over 10 seconds to see any output. Also note that some actions shown
may not actually be supported, outputting "Unknown action." when run.

Run a shadow-cljs action on this project's build id (without the colon, just `app`):
```sh
npx shadow-cljs <action> app
```
### Debug Logging

The `debug?` variable in [`config.cljs`](src/cljs/re_frame_todo_list/config.cljs) defaults to `true` in
[`dev`](#running-the-app) builds, and `false` in [`prod`](#production) builds.

Use `debug?` for logging or other tasks that should run only on `dev` builds:

```clj
(ns re-frame-todo-list.example
  (:require [re-frame-todo-list.config :as config])

(when config/debug?
  (println "This message will appear in the browser console only on dev builds."))
```

## Production

Build the app with the `prod` profile:

```sh
npm install
npm run release
```

Please be patient; it may take over 15 seconds to see any output, and over 30 seconds to complete.

The `resources/public/js/compiled` directory is created, containing the compiled `app.js` and
`manifest.edn` files.
