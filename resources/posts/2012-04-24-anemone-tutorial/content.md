{:title "Scraping a blog with Anemone (Ruby web crawler) and MongoDB"
 :permalink "8-scraping-a-blog-with-anemone-ruby-web-crawler-and-mongodb"
 :disqus-id "/posts/anemone-scraping-tutorial"}

I saw a [post on HN](http://news.ycombinator.com/item?id=3878605) demonstrating how to scrape a blog with Scrapy (Python web crawler) and MongoDB. Interested in seeing what kind of Ruby crawlers were out there, I found [Anemone](http://anemone.rubyforge.org/information-and-examples.html) and decided to replicate the functionality.

The crawler is going to: 

* Start at the blog root URL: http://bullsh.it
* Only crawl page links ("/page/4") and blog post links ("/2012/04/this-is-a-title")
* Store blog post titles and tags into a MongoDB collection
* Give you confidence spelling the word "anemone"

### Intro to Anemone

Anemone is a really simple DSL for crawling that even includes useful helpers like `page.to_absolute(link)` that turns `/foo` into `http://bullsh.it/foo`.

* Website: [http://anemone.rubyforge.org/index.html](http://anemone.rubyforge.org/index.html)
* RDocs: [http://anemone.rubyforge.org/doc/index.html](http://anemone.rubyforge.org/doc/index.html)
* Github: [https://github.com/chriskite/anemone](https://github.com/chriskite/anemone)

Here's the Anemone loop with all the included class options:

```ruby
require 'anemone'

Anemone.crawl("http://isbullsh.it") do |anemone|

  anemone.skip_links_like /PATTERN/

  # Filter a page's links down to only those you want to crawl
  anemone.focus_crawl do |page|
    page.links #=> Array of links
  end

  anemone.on_pages_like(/PATTERN/) do |page|
  end

  anemone.on_every_page do |page|
  end

  anemone.after_crawl do |page|
  end

end
```

### Filtering the crawl

I have an explicit pattern of URLs I want to crawl.

```ruby
# Patterns
POST_WITHOUT_SLASH  = %r[\d{4}\/\d{2}\/[^\/]+$]   # http://isbullsh.it/2012/04/here-is-a-title  (301 redirects to slash)
POST_WITH_SLASH     = %r[\d{4}\/\d{2}\/[\w-]+\/$] # http://isbullsh.it/2012/04/here-is-a-title/
ANY_POST            = Regexp.union POST_WITHOUT_SLASH, POST_WITH_SLASH 
ANY_PAGE            = %r[page\/\d+]               # http://isbullsh.it/page/4 
ANY_PATTERN         = Regexp.union ANY_PAGE, ANY_POST
```

I notice that the blog's links on a page never end in a trailing slash except on the actual blog post. Links without a trailing slash are 301 redirected to their with-slash version. I avoid crawling links that end in a hash `/#...` since they just jump around the same page.

`Regexp.union /pattern1/, /pattern2/, /pattern3/, ...` is great for merging a bunch of expressions together and seeing if any of them match on the target.

```ruby
anemone.focus_crawl do |page| 
  page.links.keep_if { |link| link.to_s.match(ANY_PATTERN) }
end
```

We want Anemone to follow any page or blog post link.

### Scraping the goods

Anemone is built on top of Nokogiri and provides a `page` object wrapped in Anemone helpers with a doc wrapped with Nokogiri.

```ruby
anemone.on_pages_like(POST_WITH_SLASH) do |page|
  title = page.doc.at_xpath("//div[@role='main']/header/h1").text rescue nil
  tag = page.doc.at_xpath("//header/div[@class='post-data']/p/a").text rescue nil

  if title and tag
    puts "Found Title: #{title}, Tag: #{tag}"
  end
end
```

* I specify only `POST_WITH_SLASH` so when Anemone follows a redirect on the slash-less URL, Anemone doesn't even both examining the 301 Redirect markup.
* `rescue nil` for when xpath fails. That'll do.
* Simple validation on the presence of title and tag.

We're ready to set up some storage to save our lucrative information!

### Hooking up MongoDB

```bash
$ sudo apt-get install mongodb
```

```ruby
require 'mongo'

# MongoDB
db = Mongo::Connection.new.db("scraped")
posts_collection = db["posts"]
```

* Place the setup outside of the crawler loop, of course.
* Mongo::Connection.new will connect to localhost by default
* db("scraped") specifies a database to use. If it doesn't exist, it'll create it.
* db["posts"] aka db.collection("posts") returns a collection that you can think of as the table.

### Storing our goods into MongoDB

Inserting data into Mongo couldn't be easier.

```ruby
if title and tag
  post = {title: title, tag: tag}
  puts "Inserting #{post.inspect}"
  posts_collection.insert post
end
```

In the end it's just JSON.

### Verify it works

Open up the Mongo console:

```bash
$ mongo
>> use "scraped"
>> db["posts"].find()
```

Should return your results.

### That's it

Here's the full working source:

```ruby
require 'anemone'
require 'mongo'

# Patterns
POST_WITHOUT_SLASH  = %r[\d{4}\/\d{2}\/[^\/]+$]   # http://isbullsh.it/2012/66/here-is-a-title  (301 redirects to slash)
POST_WITH_SLASH     = %r[\d{4}\/\d{2}\/[\w-]+\/$] # http://isbullsh.it/2012/66/here-is-a-title/
ANY_POST            = Regexp.union POST_WITHOUT_SLASH, POST_WITH_SLASH 
ANY_PAGE            = %r[page\/\d+]               # http://isbullsh.it/page/4 
ANY_PATTERN         = Regexp.union ANY_PAGE, ANY_POST

# MongoDB
db = Mongo::Connection.new.db("scraped")
posts_collection = db["posts"]

Anemone.crawl("http://isbullsh.it") do |anemone|
  
  anemone.focus_crawl do |page| 
    page.links.keep_if { |link| link.to_s.match(ANY_PATTERN) } # crawl only links that are pages or blog posts
  end

  anemone.on_pages_like(POST_WITH_SLASH) do |page|
    title = page.doc.at_xpath("//div[@role='main']/header/h1").text rescue nil
    tag = page.doc.at_xpath("//header/div[@class='post-data']/p/a").text rescue nil

    if title and tag
      post = {title: title, tag: tag}
      puts "Inserting #{post.inspect}"
      posts_collection.insert post
    end
  end
end
```

Anemone has more lovin' to give so check it out.
