This is my approach on an Android Gallery app. I started mostly because my previous favourite one started to annoy me with ads. After some search for a replacement I found there are not that much ***free*** gallery apps out there, so I found this might be a learning opportunity for me and so I started. It should be as simple as possible, without too much unneeded features, cloud and whatnot. Meanwhile I think it is somewhat usable and so I'm open sourcing it.  
Please excuse, that I cannot open source the commit history. I was too stupid in the beginning and stored my signing keys within the project.

Warning:
This app is in a very early state and very likely contains a lot of bugs. Use with caution and at your own risk. Especially when deleting images.

## Features

* Minimal Design
* A global image timeline
* View Folders
* Create own collections using #tags
* Give folders and tags custom colors, to quickly find them later
* Love a folder, so bring it up in the overview
* Slide through items
* Share
* Delete into a trash
* View image properties
* Functions as default image viewer

## Screenshots
![Overview](/home/simon/workspace/FreeGallery/screenshots/overview.png "Overview") ![Navigation Drawer](/home/simon/workspace/FreeGallery/screenshots/overview.png "Navigation Drawer") ![#Tag Collection](/home/simon/workspace/FreeGallery/screenshots/overview.png "#Tag Collection") ![Image Viewer](/home/simon/workspace/FreeGallery/screenshots/overview.png "Image Viewer")


## TODO's
* Lots of cleanup
* Lots of bugfixing
* Tag / Date Backup
* Maybe make Overview and Collection one activity, with Fragments
* When opened from other app as image viewer, derive folder and show as swipeable collection
* Edit image intent
* Image rotation function
* Video playback (?)
* ... A lot more ... 

## Contribution ##

I would be stoked to get contributions, so feel free to send me pull requests. Only thing for now: Just make your change as small as possible. If I don't understand it, I cannot merge it.

## How do I get set up? ##

This is an Android Studio project, so you need that to get started.

## Credits ##

Credits go to the 3rd party library authors as well to all the Android tutorials out there.

## Disclaimer ##

This app uses the free image libraries out there which are just awesome. Credits belong to the Authors of those! As well big thanks to all the People who create androd guides and tutorials online. There are great resources available and without these I would be lost in many situations.

[Glide](https://github.com/bumptech/glide)
Copyright 2014 Google, Inc. All rights reserved.

[Subsampling Scale Image View](https://github.com/davemorrissey/subsampling-scale-image-view)
Copyright 2016 David Morrissey

[Lobsterpicker](https://github.com/LarsWerkman/Lobsterpicker)
Lobsterpicker by Lars Werkmann (Design: Marie Schweiz) is licensed under Apache 2.0

[LicenseView](https://github.com/LarsWerkman/LicenseView)
LicenseView by Lars Werkmann is licensed under Apache 2.0

[Material Drawer](https://github.com/mikepenz/MaterialDrawer)
Material Drawer by Mike Penz is licensed under Apache 2.0

[Android-Iconics](https://github.com/mikepenz/Android-Iconics)
Android-Icons by Mike Penz is licensed under Apache 2.0

[ionicons](http://ionicons.com)
ionicons is licensed unter MIT license
