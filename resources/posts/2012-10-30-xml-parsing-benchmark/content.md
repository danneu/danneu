{:title "XML Parsing Benchmark: Ox vs Nokogiri (SAX vs DOM traversal) "
 :permalink "16-xml-parsing-benchmark-ox-vs-nokogiri-sax-vs-dom-traversal"
 :disqus-id "/posts/ox-vs-nokogiri-benchmark"}

I was trying to parse a 7 GB XML file with the only tool I was used to at that point: Nokogiri DOM traversal.

...Not gonna happen. After a google search, I came across this [Ox vs Nokogiri speed comparison](http://www.ohler.com/dev/xml_with_ruby/xml_with_ruby.html) which also introduced me to SAX parsing.

# Task

* Parse an 80mb XML file with 2,600,000 totalnodes (38k parent nodes, 68 nodes per parent) .
* For each user `<row>`, build a `@user` hash out of 3 specific child nodes and then print it to screen.

# Results:

Here's the test: [https://gist.github.com/3977120](https://gist.github.com/3977120)

```text
Ox DOM: 6 seconds (550mb)
Ox SAX: 6 seconds (12mb)
Nokogiri DOM: 13 seconds (900mb)
Nokogiri SAX: 24 seconds (11.8mb)
```

FWIW: My Ox SAX script parsed the original 7 GB file with 127,500,000 totalnodes in 7min, 49sec. I wasn't even going to try either of the DOM scripts and the Noko SAX script hangs up early on at about the 8,000,000th node despite the CPU staying at 100% until I quit. So there might be a problem with my Noko SAX script.

# Takeaways:

* Walking Ox's DOM tree is manual enough to where you might as well use its less-nebulous SAX API, but I did my best to include it by looking at its tests.
* It's crazy how a 80mb XML structure bloats up to 550mb and 900mb in memory once its parsed into a bunch of Ox::Element and Nokogiri::Element objects.
* It's pretty awesome parsing unlimited XML data with nothing more than 12mb of RAM.
* Ox's SAX API is better than Nokogiri's because Ox's method for extracting the value from a node is nicer. Nokogiri just calls a characters(string) method on node contents and it even includes the newlines and tab characters of the XML doc which you have to strip out.
* Noko leans on libxml, Ox rolls its own C extension.
* If you ever really needed to parse a massive XML file and its data could be mapped to a DB in any way, just COPY command that shit into Postgres in 10 seconds or something, no joke.

# SAX vs DOM traversal?

The most common way to parse an XML/HTML file is to build it into a Document Object Model (DOM) structure in memory. Nokogiri and Ox wrap each node in a `Nokogiri::Element` or `Ox::Element` Ruby object that contain metadata like the node's parent node and all of its child nodes. The browser builds up a DOM structure every time it parses an HTML document. 

It's slow and bulky. It's why an 80mb file can expand to 550mb (Ox) and 900mb (Nokogiri).

On the other hand, SAX parsing just reads sequentially through the document and fires events for things like entering a node, exiting a node, encountering attributes, and encountering node data. It doesn't keep it in memory. It's up to you to hook into these callbacks and do something with it.

It's why SAX parsing a 80mb or 7,000mb XML document only demands a constant 12mb of memory. It's glorious.
