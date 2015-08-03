andDive is a collection of Android apps in development that will allow dive and decompression planning on Android.

It's being developed in an open architecture; other developers will be able to build add-ons that enable additional functionality like support for additional decompression algorithms, profile graphing, and more that hasn't even been thought of yet. All of this will be possible through a proposed standard set of Android intents and special content providers.

Most of the backend code is complete and work on the user interface is in progress. If you are interested in contributing or finding out more information, please send me an email: divestoclimb@gmail.com

This project also houses many of the backend Java libraries used in developing [Gas Mixer](http://code.google.com/p/gasmixer) and Scuba Tanks:

  * d2c-core: handful of platform-independent utility classes
  * d2c-android: Lots of custom additions to the Android framework. Contains a custom object-relational mapping framework for mapping Android Cursors and ContentValues to/from POJO's. Also has a few custom widgets and a framework for synchronizing preferences between applications.
  * scubalib: a Java library containing lots of classes which provide an object-oriented model for dive math, including gas blending, OC/CC gas computations at depth, and the start of decompression calculations.