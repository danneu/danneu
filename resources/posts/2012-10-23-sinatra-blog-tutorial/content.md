{:title "A Simple Blog with Sinatra and Active Record ( + some useful tools)"
 :permalink "15-a-simple-blog-with-sinatra-and-active-record-some-useful-tools"
 :disqus-id "/posts/sinatra-blog-tutorial"}

# Objective

Sinatra is often suggested to newbies as an easy framework, but I imagine its lack of post-readme documentation makes it pretty daunting once you get past "Hello World". Doing something like hooking up a database is non-obvious, and writing glue code just isn't newbie friendly.

This tutorial is extracted from a comprehensive 8,000 word tutorial I wrote last weekend that explains why I'm actually doing the things that I'm doing, but it's way too aimless, verbose, and meandering. Here's a more easily digested quickstart guide.

# What we'll be building

We'll be making a blog. It won't be pretty, but it'll include these features:

* Migrations: the ability to apply and rollback changes to our database (like creating our table of posts).
* Show all of our posts on our homepage
* Individual pages for each blog post
* Appropriate forms and buttons to create/update/destroy our blog posts
* Helper methods that let us simplify our templates.
* A place to put our static pages like "About Me".
* A RESTful interface.
* Layouts, partial templates, and regular templates.
* MVC pattern
* Validations on our posts

![Screenshot of end product](http://dl.dropbox.com/u/51836583/Screenshots/6p.png)

# What our file hierarchy will look like

Some of these files/directories will be automatically generated.

```ruby
-- blog/
   +-- Gemfile                       # Lists our gem dependencies
   +-- Rakefile                      # Contains helpful tasks we can run from command line
   +-- config.ru                     # A conventional file used for deploying and integrating with some tools
   +-- app.rb                        # The guts of our Sinatra app
   +-- blog.db                       # Our SQLite3 database
   +-- views/                        # Contains our templates
       +-- layout.erb                # HTML that's displayed on every page and embeds the other templates
       +-- _delete_post_button.erb   # A partial template to extract some ugly HTML from our view
       +-- pages/              
           +-- about.erb
       +-- posts/
           +-- index.erb
           +-- show.erb
           +-- new.erb
           +-- edit.erb
```


# The gems we'll use

* `sinatra`: the framework
* `sqlite3`: the database
* `activerecord`: the interface to our database
* `sinatra-activerecord`: a bridge that lets us use Active Record
* `shotgun`: a development server that reloads our app code on each request so we don't need to restart the server to see our changes.
* `tux`: a console that lets us run Ruby code in the environment of our app/database.

```ruby
# Gemfile
source :rubygems

gem "sinatra"
gem "sqlite3"
gem "activerecord"
gem "sinatra-activerecord"

group :development do
  gem "shotgun"
  gem "tux"
end
```

Install and lock 'em: `$ bundle install`

# Our config.ru

Including a **config.ru** file is a convention that some deployment procedures and tools (like shotgun, tux, and Heroku) look for. It just includes the code of our app and tells a handler what to run.

```ruby
# config.ru
require "./app"
run Sinatra::Application
```

# Booting up the server and our console

In one terminal, start the server:

```ruby
$ shotgun
```

In another, start our console:

```ruby
$ tux
```

# Setting up the database and our Post model

Our database will be written to `blog.db` in the root of our app's directory.

Our Post model, while empty, is all Active Record needs to see to know to expose us methods that will operate on an underlying table called "posts". 

```ruby
# app.rb
require "sinatra"
require "sinatra/activerecord"

set :database, "sqlite3:///blog.db"

class Post < ActiveRecord::Base
end
```

Type `Post.all` into the tux console and it'll let you know that no posts table exists yet:

```ruby
$ tux
>> Post.all
ActiveRecord::StatementInvalid: Could not find table 'posts'
```

## Including some rake rasks

Rake tasks are helpful commands we can run from our command line. The `sinatra-activerecord` gem comes with some. Let's include them in our Rake file:

```ruby
# Rakefile
require "./app"
require "sinatra/activerecord/rake"
```

You can get a list of available rake tasks by running `$ rake -T`:

* `rake db:create_migration NAME=name_of_migration`: creates a new migration file in `db/migrate/`.
* `rake db:migrate`: apply all migrations to the database that have no been applied.
* `rake db:rollback`: undo the previous migration

## Writing our first migration

```ruby
$ rake db:create_migration NAME=create_posts
```

This will generate the directory structure and file: `db/migrate/201210231234_create_posts.rb`. That timestamp is how Active Record knows the order in which migrations should be applied to the database.

Let's open it up and write what happens to the database when this migration is applied.

* **Note**: Active Record's API is outside the scope of this tutorial. In fact, it's one of the learning curves of Ruby on Rails. Fortunately, the Ruby Guide on Active Record talks about the same gem, so check it out: [Active Record Query Interface](http://guides.rubyonrails.org/active_record_querying.html).

```ruby
class CreatePosts < ActiveRecord::Migration
  def up
    create_table :posts do |t|
      t.string :title
      t.text :body
      t.timestamps
    end
    Post.create(title: "My first post", body: "And this is the post's content.")
    Post.create(title: "How to lasso your dog", 
                body: "1. Tie a rope into a lasso. 2. Swing it over that unruly dog's torso. 3. Gently pull.")
    Post.create(title: "Top 10 coffee shops in Austin", body: "1..10: Epoch Coffee, the 24/7 coffee shop.")
  end

  def down
    drop_table :posts
  end
end
```

That gives our posts these columns:

* id (maintained by Active Record)
* title
* body
* created_at (maintained by Active Record)
* updated_at (maintained by Active Record) 

I also included three sample posts.

The up method is run when migrating, the down method is run when rolling back the migration.

## Run the migration

```ruby
$ rake db:migrate
```

It should indicate that the posts table was created.

## Verify in tux

In the tux console:

```ruby
$ tux
>> Post.count
3
>> p = Post.new(title: "My Post", body: "This is exciting!")
>> p = new_record?
true
>> p.save
>> p.new_record?
false
>> Post.count
4
```

# Our blog's homepage: a list of posts

Now that our backend is set up, let's hook it up to our website. When somebody visits our blog's root url `/`, they should see a list of our blog posts in the order in which they were created at.

```ruby
# app.rb
...
get "/" do
  @posts = Post.order("created_at DESC")
  erb :"posts/index"
end
```

* `@instance_variables` are passed into the view layer. We assign them here.
* `erb` declares which template to render.

## Our post index template

```html 
# views/posts/index.erb
<h1>Welcome to my blog!</h1>
<ul>
<% @posts.each do |post| %>
  <li>
    <h2><a href="/posts/<%= post.id %>"><%= post.title %></a>
    <span><%= pretty_date(post.created_at) %></span>
    </h2>
  </li>
<% end %>
</ul>
```

## Our layout

Templates are embedded inside of layouts, and layouts give us a place to put the code that's common between every template so that our templates can just concern themselves with what's changed. 

```html
# views/layout.erb
<html>
<head>
  <title><%= title %></title>
  <% puts request.inspect %>
</head>
<body>
  <ul>
    <li><a href="/">Home</a></li>
    <li><a href="/about">About Me</a></li>
  </ul>
  <%= yield %>
</body>
</html>
```

The `yield` method is where templates are embedded.

# Some helpers

You're notice two methods in the about two files:

* `<%= title %>`
* `<%= pretty_date(post.created_at) %>`

These are defined in a `helpers` block in our app.rb which are available to any route and view. They let us extract code and simplify our view files. 

Here's what they look like:

```ruby
# app.rb
...
helpers do
  # If @title is assigned, add it to the page's title.
  def title
    if @title
      "#{@title} -- My Blog"
    else
      "My Blog"
    end
  end

  # Format the Ruby Time object returned from a post's created_at method
  # into a string that looks like this: 06 Jan 2012
  def pretty_date(time)
   time.strftime("%d %b %Y")
  end

end 
...
```

# The rest of our routes

Without getting too deep in describing each one, here are all of our routes at once.

```ruby
# app.rb

...

# Get all of our routes
get "/" do
  @posts = Post.order("created_at DESC")
  erb :"posts/index"
end

# Get the New Post form
get "/posts/new" do
  @title = "New Post"
  @post = Post.new
  erb :"posts/new"
end

# The New Post form sends a POST request (storing data) here
# where we try to create the post it sent in its params hash.
# If successful, redirect to that post. Otherwise, render the "posts/new"
# template where the @post object will have the incomplete data that the 
# user can modify and resubmit.
post "/posts" do
  @post = Post.new(params[:post])
  if @post.save
    redirect "posts/#{@post.id}"
  else
    erb :"posts/new"
  end
end

# Get the individual page of the post with this ID.
get "/posts/:id" do
  @post = Post.find(params[:id])
  @title = @post.title
  erb :"posts/show"
end

# Get the Edit Post form of the post with this ID.
get "/posts/:id/edit" do
  @post = Post.find(params[:id])
  @title = "Edit Form"
  erb :"posts/edit"
end

# The Edit Post form sends a PUT request (modifying data) here.
# If the post is updated successfully, redirect to it. Otherwise,
# render the edit form again with the failed @post object still in memory
# so they can retry.
put "/posts/:id" do
  @post = Post.find(params[:id])
  if @post.update_attributes(params[:post])
    redirect "/posts/#{@post.id}"
  else
    erb :"posts/edit"
  end
end

# Deletes the post with this ID and redirects to homepage.
delete "/posts/:id" do
  @post = Post.find(params[:id]).destroy
  redirect "/"
end

# Our About Me page.
get "/about" do
  @title = "About Me"
  erb :"pages/about"
end
```

# All of our templates

And here are the templates they render:

```html
# views/posts/new.erb
<h1>New Post</h1>
<form action="/posts" method="post">
  <label for="post_title">Title:</label><br />
  <input id="post_title" name="post[title]" type="text" value="<%= @post.title %>" />
  <br />

  <label for="post_body">Body:</label><br />
  <textarea id="post_body" name="post[body]" rows="5"><%= @post.body %></textarea>
  <br />

  <input type="submit" value="Create Post" />
</form>   
```

```html
# views/posts/show.erb
<h1><%= @post.title %></h1>
<p><%= @post.body %></p>
```

```html
# views/posts/edit.erb
<h1>Edit Post</h1>
<form action="/posts/<%= @post.id %>" method="post">
  <input type="hidden" name="_method" value="put" /> 

  <label for="post_title">Title:</label><br />
  <input id="post_title" name="post[title]" type="text" value="<%= @post.title %>" />
  <br />

  <label for="post_body">Body:</label><br />
  <textarea id="post_body" name="post[body]" rows="5"><%= @post.body %></textarea>
  <br />

  <input type="submit" value="Edit Post" />
</form>   
```

* Note: I explained in the routes above that our Edit Post form sends a PUT request, but we use unfortunately the HTML spec only accepts "GET" or "POST" in a `<form>`'s method attribute. So we instead let Sinatra know that we want this to be a PUT request by specifying it in a hidden form input called `_method` with a value of `put`. You also do the same thing in Ruby on Rails and other frameworks.

```html
# views/pages/about.erb
<h1>About Me</h1>
<p>My name is Dan.</p>
```

# A navigation bar that lets us add/edit/delete posts

We want a list of links following us around to each page, so we'll put it in our layout. So far, our layout already has a convenient link back to our homepage, but we also want links to our About Me page, the New Post form, the Edit Post form of the current post, and a Delete Post button for the current post.

Here's our completed layout with those links added:

```html
# views/layout.erb
<html>
<head>
  <title><%= title %></title>
</head>
<body>
  <ul>
    <li><a href="/">Home</a></li>
    <li><a href="/about">About Me</a></li>             
    <li><a href="/posts/new">New Post</a></li>                             
    <% if post_show_page?  %>
      <li><a href="/posts/<%= @post.id %>/edit">Edit Post</a></li>
      <li><%= delete_post_button(@post.id) %></li>
    <% end %>
  </ul>
  <%= yield %>
</body>
</html>
```

Note that we only want to see "Edit Post" and "Delete Post" if we're viewing a post, so I created a `post_show_page?` helper method that checks the current URL:

```ruby
# app.rb

helpers do
...
  def post_show_page?
    request.path_info =~ /\/posts\/\d+$/
  end
...
end
```

It only gets evaluated to true in that if-statement if the current path is **/posts/{number}**, like **/posts/6**.

I'll explain the delete button next.

# How to make a delete button

You'll also notice that I'm using another helper called `delete_post_button` and you pass in the ID of a post.

Basically, we need a link that can send a DELETE request to our `delete "posts/:id"` route. However, an `<a>` tag can only send a GET request. Much like on our Edit Post form, we have to use that `<form>` method override trickery to make our DELETE button.

In other words, we have to make a small `<form>`. I didn't want to bloat up the view with that markup, so I wrote a helper.

Here's what that helper looks like:

```ruby
# app.rb
...
helpers do
  ...
  def delete_post_button(post_id)
    erb :_delete_post_button, locals: { post_id: post_id}
  end
end
```

All it does is render a partial template located at `views/_delete_post_button.erb`. The underscore (_) is a convention I took from Ruby on Rails to distinguish partial templates (templates I want to embed in other templates) from regular templates.

The `locals` has is used to pass the post id from view view into the final erb partial. We could leave off the locals hash complete and make _delete_post_button.erb except that @post.id will exist, but that's not very robust and a meek little partial shouldn't be bothered to know that.

```html
# views/_delete_post_button.erb
<form action="/posts/<%= post_id %>" method="post">
  <input type="hidden" name="_method" value="delete" /> 
  <input type="submit" value="Delete Post" />
</form>
```

# Adding some validations

We have some checks in our routes to see if the post is successfully created/updated, but we don't have a way for a post to fail when trying to save it to the database. 

Let's ensure that a post must:

* Have a body
* Have a title that's > 3 characters long

Pop it into our model:

```ruby
# app.rb
...
class Post < ActiveRecord::Base
  validates :title, presence: true, length: { minimum: 3 }
  validates :body, presence: true
end
```

Verify in the tux console:

```ruby 
$ tux
>> p = Post.new
>> p.valid?
false
>> p.errors.messages 
{:title=>["can't be blank", "is too short (minimum is 3 characters)"], :body=>["can't be blank"]}
>> p.save
false
>> p.title = "I have a title now!"
>> p.body = "And I also have a body! Thanks!"
>> p.valid?
true
```

Verify on our New Post and Edit Post forms. If you try to save a post with incomplete data, it should drop you back on the form with your data still filled out.

# That's it!

Since I extracted this guide from a much longer more verbose guide, I may have cut too many corners. Email me at `danrodneu@gmail.com` if you have questions or comments.
