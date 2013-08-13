{:title "Meteor tutorial for fellow noobs: Adding features to the Leaderboard demo"
 :permalink "6-meteor-tutorial-for-fellow-noobs-adding-features-to-the-leaderboard-demo"
 :disqus-id "/posts/meteor-tutorial-leaderboard-demo"}

[Meteor](http://www.meteor.com/) is a javascript framework that just launched. Its screencasts demonstrate effortless real-time applications and I don't possess the technical understanding to explain it more technically that using adjectives like "real-time". But it's X-treme and the allure of it being so new and so instantly popular encouraged me to give it a go.

The [Hacker News submission](http://news.ycombinator.com/item?id=3824908) announcing its launch has over 1,100 votes at the moment.

Intoxicated with opportunistic greed, this may be my only chance to write something someone will ever read.

## Leaderboard demo

I'll be adding some functionality to the [Leaderboard demo](http://meteor.com/examples/leaderboard) that comes with the application:

* a form to add new players
* a button to remove existing players
* new player name validation

You can install Meteor and generate the Leaderboard demo with these commands:

```shell
$ curl install.meteor.com | sh
$ meteor create --example leaderboard
$ cd leaderboard
$ meteor 
```

## Adding a New Player form

I'd like a simple form for adding new players.

![the "add new player" form](http://i.imgur.com/3jqpH.png)

First, I made a new template with a text field and a button.

```html
<!-- leaderboard.html -->
<template name="new_player">
  <div class="new_player">
    <input id="new_player_name" type="text" />
    <input type="button" class="add" value="Add Player" />
  </div>
</template>
```

**Note**: due to a current [bug that they're working on](http://stackoverflow.com/a/10113151/511200), the `div` wrapper is necessary because event selectors don't work on top-level elements. I punched so many pillows trying to understand why my click events wouldn't work.

I'll add a line to the `leaderboard` template to nest it at the bottom of the page:

```html
<!-- leaderboard.html -->
<template name="leaderboard">
  ...
  {{> new_player }} 
</template>
```

Now I'll add a click event for the `Add Player` button.

```javascript
// leaderboard.js
Template.new_player.events = {
  'click input.add': function () {
    var new_player_name = document.getElementById("new_player_name").value;
    Players.insert({name: new_player_name, score: 0});
  }
};
```

Notice that the event is scoped to the template name I just made.

When the `Add Player` button is clicked, I grab the value of the
`#new_player_name` input field and then insert it into the Players
collection with a starting score of zero.

You'll see that it's declared at the top of the javascript file:

```javascript
// leaderboard.js
Players = new Meteor.Collection("players");
```

As players are added to the collection, Meteor automatically updates the
template to reflect the changes.

## Reactivity

Meteor handles automatic recomputation for you and refers to it as
Reactivity.

[Meteor docs: Reactivity](http://docs.meteor.com/#reactivity) - Quick explanation

But the main thing to understand is this:

> These Meteor functions run your code in a reactive context:
> 
<ul><li>Meteor.ui.render and Meteor.ui.chunk</li>
<li>Meteor.autosubscribe</li>
<li>Templates</li></ul>

> And the reactive data sources that can trigger changes are:
>
<ul>
<li>Session variables</li>
<li>Database queries on Collections</li>
<li>Meteor.status</li></ul>

In other words: 

* Templates are automatically recomputated by Meteor 
* and changes to the players collection automatically triggers the recomputation.

When I introduce validation, I'll be using a Session variable to trigger error message updates.

## Buttons to remove players

Here's what it'll look like:

![delete buttons next to player names](http://i.imgur.com/EU8mj.png)

To make the button, I added one line to the `player` template that's looped to create each row of the player list:

```html
<!-- leaderboard.html -->
<template name="player">
  <div class="player {{selected}}">
    <input type="button" value="X" class="delete" /> <!-- here it is -->
    <span class="name">{{name}}</span>
    <span class="score">{{score}}</span>
  </div>
</template>
```

And now I just need to add a click event to remove the player from the collection:

```javascript
// leaderboard.js
Template.player.events = {
  'click input.delete': function () { // <-- here it is
    Players.remove(this._id);
  },
  'click': function () {
    Session.set("selected_player", this._id);
  }
};
```

## New player name validation

The main criticism of Meteor involves its complete lack of security. The full database API is accessible from the client's console. However, this is
indeed just a preview of Meteor and the creators have explained that such an
obvious necessity hasn't eluded them.

I'll create two validations for any new name:

1. Can't be blank
2. Can't already exist in the collection

An error message will show up above the creation field if validation fails:

![a "name too short" failure](http://i.imgur.com/rNKDH.png)
![a "player already exists" failure](http://i.imgur.com/GQ35e.png)

Let's add an error message condition to our `new player` template:

```html
<!-- leaderboard.html -->
<template name="new_player">
  {{#if error}}
    <span id="error" style="color: red;">
      {{error}} 
    </span>
  {{/if}}
  <div class="new_player">
    <input id="new_player_name" type="text" />
    <input type="button" class="add" value="Add Player" />
  </div>
</template>
```

* For `{{#if error}}` and `{{error}}` to work, we need a matching `Template.new_player.error` to
return either `undefined` or the actual error message.
* For template logic involving `{{error}}` to be reactive and automatically
update, it needs to be backed by one of the data sources listed above in the
Reactivity section.

If that didn't make sense, it will when I post the code.

I chose to use a Session variable I named `error`. To demonstrate:

```javascript
>>> Session.keys
Object { }

>>> Session.set("error", "Name can't be blank");
>>> Session.keys
Object { error="Name can't be blank" }

>>> Session.get("error");
"Name can't be blank"

>>> Session.set("error", undefined)
>>> Session.keys
Object { }
```

That's pretty much it, but here's the [Session API doc](http://docs.meteor.com/#session).

Let's address the aforementioned `{error}` issue and expose a matching
Template variable:

```javascript
// leaderboard.js
Template.new_player.error = function () {
  return Session.get("error");
};
```

`{error}` in the template html expects an `error()` function and we'll
just return the Session variable. If it's `undefined`, the `{#if error}`
check fails.

## Using a Validation object

After a brief Google tour of javascript syntax, I
decided to use an object literal to organize our validation logic.

Here's the primitive API I came up with:

* `Validation.clear()` will destroy the Session variable (`undefined`).
* `Validation.set_error(message)` will assign the Session variable.
* `Validation.valid_name(name)` will return `true` or `false`, run all the
 validation checks, and clear/set the Session variable. It does it all,
 folks.
* `Validation.player_exists(name)` returns `true` or `false` and actually queries the
 collection to see if the name already exists.

I clearly didn't spend much time on the design, but it does the job and helps 
me sleep at night knowing that my growing armada of helper functions are at 
least sitting behind an arbitrary object variable.

Here's the `Validation` object in full ensemble:

```javascript
// leaderboard.js
Validation = {
  clear: function () { 
    return Session.set("error", undefined); 
  },
  set_error: function (message) {
    return Session.set("error", message);
  },
  valid_name: function (name) {
    this.clear();
    if (name.length == 0) {
      this.set_error("Name can't be blank");
      return false;
    } else if (this.player_exists(name)) {
      this.set_error("Player already exists");
      return false;
    } else {
      return true;
    }
  },
  player_exists: function(name) {
    return Players.findOne({name: name});
  }
};
```

Now we can add a quick validation check on our existing `Add Player` click event before adding a new player to the collection.

```javascript
// leaderboard.js
Template.new_player.events = {
  'click input.add': function () {
    // notice the added trim()
    var new_player_name = document.getElementById("new_player_name").value.trim();

    // here's the valid_name check
    if (Validation.valid_name(new_player_name)) {
      Players.insert({name: new_player_name, score: 0});
    } 
  }
};
```

Note that the new player name now gets a `trim()` before validation so a string of spaces remains invalid.

## That's it

I'm pretty sure that's everything.

I'll update this post as I add more features and discover better programming/javascript practices.

I have yet to add comments to my blog, but I'd love feedback (especially regarding the javascript!). For now, please email me at `danrodneu@gmail.com`.
