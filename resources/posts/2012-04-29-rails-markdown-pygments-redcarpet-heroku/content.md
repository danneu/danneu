{:title "Rails 3.2 + Markdown + Pygments + RedCarpet + Heroku"
 :permalink "9-rails-3-2-markdown-pygments-redcarpet-heroku"
 :disqus-id "/posts/rails-markdown-pygments-redcarpet-heroku-tutorial"}

The [Markdown with Redcarpet](http://railscasts.com/episodes/272-markdown-with-redcarpet) Railscast is outdated, so I thought I'd share my solution.

### Parsing Markdown with Redcarpet

My blog stores a post's Markdown in `@post.body` and then processes it to
html in `@post.rendered_body` each time a post is created or updated (with a
`before_save` filter).

I decided to start using Github's Redcarpet instead of the popular RDiscount for Markdown-to-HTML rendering. Redcarpet adds some sensible defaults and nice extensions on top of standard Markdown.

```ruby
# Gemfile
gem 'redcarpet'
```

```ruby
# app/models/post.rb
class Post < Model
  before_save :render_body

  private
  def render_body
    require 'redcarpet'
    renderer = Redcarpet::Render::HTML.new
    extensions = {fenced_code_blocks: true}
    redcarpet = Redcarpet::Markdown.new(renderer, extensions)
    self.rendered_body = redcarpet.render self.body
  end
end
```

I separated the code to help indicate what's going on. There's a list of other extensions on the [Redcarpet Github](https://github.com/tanoku/redcarpet).

`fenced_code_blocks: true` will tell Redcarpet to turn this Markdown:

```ruby
~~~ ruby
puts "hello"
~~~
```

into this HTML:

```html
<code lang="ruby">
puts "hello"
</code>
```

`@post.rendered_body` is now correctly saving the HTML version of the Markdown post on each save, but now I'll add colorful syntax highlighting.

### Syntax Highlighting with Pygmentize

Pygments is a popular syntax highlighting library written in Python. A gem called [Pygmentize](https://github.com/djanowski/pygmentize) packages all the necessary files you need. Since Heroku's Cedar Stack can run Python files, Pygmentize Just Works.

```ruby
# Gemfile
gem 'pygmentize'
```

Let's revisit the above code and stick Pygmentize into the flow so that it processes any `<code>` block that Redcarpet renders.

```ruby
# app/models/post.rb
class Post < Model
  before_save :render_body

  private
  def render_body
    require 'redcarpet'
    # renderer = Redcarpet::Render::HTML.new
    renderer = PygmentizeHTML
    extensions = {fenced_code_blocks: true}
    redcarpet = Redcarpet::Markdown.new(renderer, extensions)
    self.rendered_body = redcarpet.render self.body
  end
end

class PygmentizeHTML < Redcarpet::Render::HTML
  def block_code(code, language)
    require 'pygmentize'
    Pygmentize.process(code, language)
  end
end
```

There we go. All we did was extend the renderer we were already using but add an intermediate Pygmentize step whenever the renderer encounters a `<code>` block.

Pygmentize basically chops up code into a bunch of CSS-classes spans. All you have to do is get a Pygments-friendly stylesheet which is easy enough to google. I'm using a partial CSS port of [Solarized][solar], my favorite Vim theme.
