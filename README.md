wpimgsearch
===========

This is the result of a small project/challenge from Wikimedia to build a native Android app that will search Wikipedia
for a given term and return the results as images.

The project attempts to cache search results in memory, and caches file image files it downloads into the app's cache
directory (returned by Android).

TODO
====

There are many different things that I'd like to see this project do, but I ran out of time in the challenge (I even went
over to get it into a nice enough state to demo)

One thing that I'd like to add is the ability to search by voice command.  Android's voice to text is rather good,
so it's not terribly difficult to add.

The UI interactions I wanted to have were highly gesure dependent, and would have required adding those, as well as a mini
tutorial to let people know about them.  The gestures I envisioned the app including were:

- Touching a page wouldn't launch a browser/the Wikipedia app, but would zoom in to show a larger version of the image
- Swiping left or right would "flip" the image over and display some basic information about the page
  (At least the title and URL, but maybe even the "page summary"?)
- "Pushing" away would put the image back to its thumbnail size
- "Pulling" toward the user would send a URI browse intent

Lower on the list of things that would be nice to add would be some way to add a background to the default/no image pages
so they're not all just plain black & white.
