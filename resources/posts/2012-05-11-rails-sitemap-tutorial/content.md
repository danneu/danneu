{:title "Generating and submitting a sitemap.xml with Rails"
 :permalink "13-generating-and-submitting-a-sitemap-xml-with-rails"
 :disqus-id "/posts/sitemap-with-rails-tutorial"}

You can see this blog's sitemap at [danneu.com/sitemap.xml](http://danneu.com/sitemap.xml). Any time a post is created, it updates the sitemap.

Some tools I use include page caching, a sweeper, an observer, a custom
logger, and an xml builder. (My expensive SEO consultant says I gotta get that
keyword density up.)

### The Sitemap Protocol

[sitemaps.org/protocol.html](http://www.sitemaps.org/protocol.html)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
   <url>
      <loc>http://www.example.com/</loc>  required
      <lastmod>2005-01-01</lastmod>       optional
      <changefreq>monthly</changefreq>    optional
      <priority>0.8</priority>            optional
   </url>
   <url>
     ...loop through your URLs...
   </url>
</urlset> 
```

### Building the sitemap

#### 1. The route

The conventional location for a simple sitemap seems to be `domain.com/sitemap.xml`. I default `params[:format]` to "xml" and map the route to the index action of sitemaps_controller.rb.

```ruby
# config/routes.rb
Grinch::Application.routes.draw do
  get "sitemap.xml" => "sitemaps#index", as: "sitemap", defaults: { format: "xml" }
  ...
end
```

#### 2. The controller (+ caching the sitemap)

If you're unfamiliar with Rails caching, check out: [Rails Guides: Caching with Rails](http://guides.rubyonrails.org/caching_with_rails.html)

Note: Rails doesn't cache by default in development mode. If you want to test page caching locally, you'll need to enable it:

```ruby
# config/environments/development.rb
config.action_controller.perform_caching = true
```

But here's a quick crash course:

Page caching means the full response is written to a static file in `public/`. Future requests won't even hit Rails because, generally, servers are configured to look in `public/` before passing the request to Rails. This is why you have to remove `public/index.html` in a new Rails app before you can see your root action.

`caches_page :index` will tell Rails to write to a `sitemap.xml` if it doesn't exist. The route tells Rails where to write it hierarchically in the `public` directory:

* `get "sitemap.xml"` --> `public/sitemap.xml`
* `get "sitemaps/sitemap.xml"` --> `public/sitemaps/sitemap.xml`

```ruby
# app/controllers/sitemaps_controller.rb
class SitemapsController < ApplicationController
  caches_page :index
  def index
    @static_paths = [about_me_path, projects_path]
    @posts = Post.all
    respond_to do |format|
      format.xml
    end
  end
end
```

Like any other controller action, it'll send those @vars to an expected `views/sitemaps/index.xml`

#### 3. The builder

`Builder` ships with Rails. It's akin to any other compilers/templaters you may be used to like `.css.scss` and `.html.erb`.

Builder initializes an `xml` object that you can just add on to.

```ruby
# app/views/sitemaps/index.xml.builder
xml.urlset(xmlns: "http://www.sitemaps.org/schemas/sitemap/0.9") do
  @static_paths.each do |path|
    xml.url do
      xml.loc "#{GRINCH['root_url']}#{path}"
      xml.changefreq("monthly")
    end
  end
  @posts.each do |post|
    xml.url do
      xml.loc "#{GRINCH['root_url']}#{url_for(post)}"
      xml.lastmod post.updated_at.strftime("%F")
      xml.changefreq("monthly")
    end
  end
end
```

#### 4. Expiring the cache when posts are created/destroyed.

Rails needs to be told when to expire (delete) a cached page. We can do
that with a [Sweeper](http://api.rubyonrails.org/classes/ActionController/Caching/Sweeping.html).

> Sweepers are the terminators of the caching world and responsible for expiring caches when model objects change. They do this by being half-observers, half-filters and implementing callbacks for both roles.

```ruby
# app/sweepers/sitemap_sweeper.rb
class SitemapSweeper < ActionController::Caching::Sweeper
  observe :post

  def sweep(post)
    expire_page(sitemap_path)
  end

  alias_method :after_create, :sweep
  alias_method :after_destroy, :sweep
end
```

`expire_page()` is the opposite of the `caches_page()` we used in the
controller. Any time a Post is created/destroyed, Rails will delete the
file specified in our sitemap_path route. A fresh sitemap.xml will be
cached on the next request.

For an app with a large sitemap, you'd want to start rebuilding it
immediately so a search engine doesn't have to wait. Please, think of the crawlers.

#### Simples

Great.Now a fresh sitemap.xml is located at domain.com/sitemap.xml even as we create and destroy posts.  

But now let's tell search engines about it when an updated sitemap is
available.

### Pinging search engines

#### 1. The pinger

```ruby
# app/models/sitemap_pinger.rb
class SitemapPinger 
  SEARCH_ENGINES = {
    google: "http://www.google.com/webmasters/tools/ping?sitemap=%s",
    ask: "http://submissions.ask.com/ping?sitemap=%s",
    bing: "http://www.bing.com/webmaster/ping.aspx?siteMap=%s"
  }

  def self.ping
    SitemapLogger.info Time.now
    SEARCH_ENGINES.each do |name, url|
      request = url % CGI.escape("#{GRINCH['root_url']}/sitemap.xml")  
      SitemapLogger.info "  Pinging #{name} with #{request}"
      if Rails.env == "production"
        response = Net::HTTP.get_response(URI.parse(request))
        SitemapLogger.info "    #{response.code}: #{response.message}"
        SitemapLogger.info "    Body: #{response.body}"
      end
    end
  end
end
```

You might want to round off your search engine list with the big boys like
AltaVista, HotBot, and Lycos, but I wasn't about to get cocky.

#### 2. The logger

To ensure my pinger even does anything, I log the responses to
`logs/sitemap.log`. I also don't want to ping the search engines during
development/testing any time I mess with Posts, so I tell it to only ping in
production.

To set up a new logger, create an initializer.

```ruby
log_path = File.join(Rails.root, 'log/sitemap.log')
log_file = File.open(log_path, 'a')
log_file.sync = true
SitemapLogger = Logger.new(log_file)
```

#### 3. Triggering the ping

We have our pinging mechanism set up, but now we need to actually invoke the pinger
when we have some fresh intel for our search engine friends.

I use an
[Observer](http://api.rubyonrails.org/classes/ActiveRecord/Observer.html).

> Observer classes respond to life cycle callbacks to implement trigger-like behavior outside the original class. This is a great way to reduce the clutter that normally comes when the model class is burdened with functionality that doesn't pertain to the core responsibility of the class.

```ruby
# app/observers/sitemap_observer.rb
class SitemapObserver < ActiveRecord::Observer
  observe :post

  def ping(post)
    SitemapPinger.ping
  end

  alias_method :after_create, :ping
  alias_method :after_destroy, :ping
end
```

However, unlike sweepers, we must register our observer before it will be
invoked.

```ruby
# config/application.rb
config.active_record.observers = :sitemap_observer
```

#### Boom

There we have it. Our observer will call our SitemapPinger.ping whenever
posts are created/destroyed (and our sitemap.xml is destroyed). 

To verify it all works in development: 

* Remove the `if Rails.env == "production"` condition in
the SitemapPinger
* Ensure `config.action_controller.perform_caching = true`
* Create/destroy a few posts
* Check `log/sitemap.log` for updates
* Ensure `rooturl.com/sitemap.xml` works
