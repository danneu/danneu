
# danneu

My work-in-progress-but-functional Jekyll-inspired blog.

- Live demo: [danneu.com](http://www.danneu.com/)

It's the idealogical successor of my Jekyll blog: [github.com/danneu/danneu.github.com](https://github.com/danneu/danneu.github.com).

## How it works

I wanted every post to be contained in its own subfolder so that images and even custom javascript, stylesheets, and live demos could also live within that folder.

The only required file for each post is a `content.md` Markdown file.

```plain
resources/
└── posts
    ├── 2012-02-02-darkstrap
    │   ├── content.md
    │   └── img
    │       └── comparison.png
    ├── 2012-04-11-meteor-tutorial
    │   └── content.md
    ├── 2012-04-24-anemone-tutorial
    │   └── content.md
    ├── 2012-04-29-rails-markdown-pygments-redcarpet-heroku
    │   └── content.md
    ├── 2012-05-11-rails-sitemap-tutorial
    │   └── content.md
    ├── 2012-06-05-mocha-coffeescript-tutorial
    │   └── content.md
    ├── 2012-10-23-sinatra-blog-tutorial
    │   └── content.md
    ├── 2012-10-30-xml-parsing-benchmark
    │   └── content.md
    └── 2013-02-09-discourse-plugin-tutorial
        ├── content.md
        └── img
            └── pervasive-banner-screenshot.png
```

At the top of each `content.md` post is a map of options sort of like the options at the top of a Jekyll post Markdown file.

Here's an example of the barebones post:

```plain
{:title "My First Post!"
 :permalink "my-first-post"}
 
# Hello World

I'm glad you're here to begin this adventure with me.
```

That post will be available at `example.com/posts/my-first-post`.

## TODO

- **Extract static-file generator into standalone tool**

This project is actually a test-bed of ideas that I'd like to eventually fasten into Jekyll-like tool.

The working title of my tool on localhost is "klobb".

Like,

```bash
$ klobb new myblog
(klobb creates barebones project in a `myblog/` folder)

$ cd myblog
$ klobb generate
(klobb parses myblog's posts into `myblog/site/`)

$ klobb server 3000
(klobb conveniently serves your blog at `http://localhost:3000/`)
```

If I embed Jetty as the web server, then the `server` command should be perfectly fine for production.

- **Read `resources/posts/*/` from within the .jar**

I haven't fully wrapped my head around the issue, but since I can't do File I/O operations on files within the .jar, this project only works if I run the compiled .jar from the app's root:

```bash
$ cd ~/Code/Clojure/danneu
$ lein uberjar
$ java -jar target/danneu-standalone.jar
```

I haven't fully wrapped my head around what's going on, but I believe this only works because the File I/O operations (grabbing all the posts within the resources folder) are working off relative paths.

The File operations don't work if I launch the .jar from an absolute path or a path outside the app's root:

```bash
$ java -jar ~/Code/Clojure/danneu/target/danneu-standalone.jar
```

The File operations in the `danneu.post` namespace (`src/danneu/post.clj`) fail and the blog appears as if it has no posts.

Looks like there might be a solution using Java's [ZipInputStream](http://docs.oracle.com/javase/7/docs/api/java/util/zip/ZipInputStream.html).

See this [Stackoverflow answer](http://stackoverflow.com/questions/5419125/using-java-api-from-clojure-reading-zip-file).

I'm adamant on the concept of dynamically generating posts from Markdown files in the `resources/posts/` folder because my approach is to consider static HTML files as a caching layer. Jekyll's approach is to `watch` for changes in your source files to know when to regenerate associated HTML files. My approach is to simply toggle off HTML generation during development.

But this may change when I make more progress extracting the klobb tool.

- **Add memory caching** 

At the moment, it hilariously parses the Markdown on every request.

In the short term, I can solve this trivially by just caching the Markdown->HTML in memory.

But my plan is to eventually spit out HTML files. My two inspirations for this project are Jekyll and the page-caching of Ruby on Rails. So, like Rails page-caching, I want to be able to hit the Clojure stack until I decide to cache the page in HTML at which point the entire request is served by Nginx. Ruby on Rails does this beautifully and that's some swagger that needs to be jacked.

- **Move to server-side syntax highlighting**

For now, I'm using Javascript for syntax highlighting: [SyntaxHighlighter](http://alexgorbatchev.com/SyntaxHighlighter/).

And only because the Markdown parser integrates with it: [github.com/yogthos/markdown-clj](https://github.com/yogthos/markdown-clj).

But this introduces a Javascript dependency and forces every client to do work that the server can do once.

- **Use my own Markdown parser (Or give up and contribute to an existing Clojure parser)**

While I've got the ball rolling (https://github.com/danneu/klobbdown), the grammar is not very pretty.

I basically want to have a Markdown parser that I can easily extend with some features I'd like for my static site generator. I have a few issues with the Java/Clojure solutions I've used.

But I'll first have to evaluate whether it'll be easier to struggle with my Klobbdown project or contribute to an existing Clojure parser.
