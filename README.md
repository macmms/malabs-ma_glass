MA for Glass™
=============
- - -

TABLE OF CONTENTS
-----------------

1. [Authors & Contributors][head0100]  
2. [Introduction][head0200]  
3. [Requirements][head0300]  
4. [Setup][head0400]  
5. [Installation][head0500]  
6. [Running][head0600]  
  6.1 [Main Menu][head0601]  
  6.2 [Work Orders][head0602]  
  6.3 [Scheduled Maintenance][head0603]  
  6.4 [Scan Asset QR Code][head0604]  
  6.5 [Generate Asset][head0605]  
  6.6 [View More][head0606]  
  6.7 [See Tasks][head0607]  
  6.8 [Change Status ][head0608]  
  6.9 [Create Work Request][head0609]  
7. [Licensing][head0700]  
9. [Change Log][head0800]  
  [v1.0.3][v103]  
  [v1.0.2][v102]  
  [v1.0.1][v101]  
  [v1.0.0][v100]


<br>

1 AUTHORS &amp; CONTRIBUTORS
----------------------------

v1.0.0 Created August 14, 2015 by Jake Uskoski  
v1.0.1 Created August 18, 2015 by Jake Uskoski  
v1.0.2 Created August 20, 2015 by Jake Uskoski  
v1.0.3 Created August 20, 2015 by Jake Uskoski

[Back to Top][BackToTop]
<br>

2 INTRODUCTION
--------------

MA for Glass is a Google Glass application prototype for use in conjunction with the
Maintenance Assistant Computerized Maintenance Management System (MA CMMS). MA
Glass is capable of viewing active work orders, tasks, scheduled maintenance, and
assets, as well as generating new assets and work orders, linked to the paired
CMMS. It is meant as a tool for maintenance staff during maintenance and walking,
as a mobile, hands-free alternative to the MA Mobile App and the browser CMMS.

Glass is a trademark of Google Inc.

[Back to Top][BackToTop]
<br>

3 REQUIREMENTS
--------------

An Android Development Environment is needed to build the application.
[Android Studio][AndStd] is recommended.

[ZXing][ZXing] ("Zebra Crossing") is also required in order to scan QR codes. To learn
about downloading and building the ZXing application for glass, visit their
[Getting Started][ZXstp] page on their [GitHub repository][ZXing].

This application also requires the Java SE Runtime Environment 8, and uses the
Maintenance Assistant CMMS client, which depends on the following external
libraries:

* Apache Commons Codec
* Apache Commons Logging
* Apache Jakarta HttpClient
* Jackson JSON processor

These libraries, along with the Maintenance Assistant CMMS client, are packaged
in the "lib" folder within the project.

[Back to Top][BackToTop]
<br>

4 SETUP
-------

To set up Android Studio for Google Glass, see the following page:

&nbsp;&nbsp;&nbsp;[GDK Quick Start][AndStp]
  
On the lower half of the page, there is a setup for beginners. After following
the steps for preparing your Android Studio, you can either import MA for Glass
directly from GitHub using the following link:

&nbsp;&nbsp;&nbsp;[https://github.com/macmms/malabs-ma_glass.git][GitRep]  
&nbsp;&nbsp;&nbsp;*Note: The project is built with Gradle.*

Or by downloading a ZIP version of the repository, and extracting it to a folder
on your computer, from the following link:

&nbsp;&nbsp;&nbsp;[https://github.com/macmms/malabs-ma_glass/archive/master.zip][GitZip]
  
With the files extracted, the project can be imported from your file directory
as a gradle project.

[Back to Top][BackToTop]
<br>

5 INSTALLATION
--------------

The MA for Glass application needs API credentials from the MA CMMS in order to
function. Three keys and a URL to the CMMS are required. To learn about getting
your API keys, go to the web page:

&nbsp;&nbsp;&nbsp;[https://www.maintenanceassistant.com/api/docs/guide.html][APIdoc]

and see the section "Getting your API Access Keys".

Next, open the file called ["strings.xml"][STR]. The file is located in the "values"
folder, which is within the "res" folder. The URL and keys go into the slots
labelled accordingly (the slots are the open spaces between the ">" and "<"
where there is placeholder text explaining which slot is which) at the end of
the file. Make sure there are no spaces in the slots.

The application is now ready to be uploaded to Google Glass. Make sure that your
Glass is in debug mode, and that it is plugged into your computer. If you
followed the beginner steps in the [Quick Start guide][AndStp], you should
already be set up and ready to go.

In Android Studio, build and run the application. Building the application can
be done from the top menu "Build", selecting the option "Make Project" or "Clean
Project". Running the application can be done from the top menu "Run", selecting
the option "Run". After it finishes building, you should be prompted to select
which device you want to run the application on. Select your Google Glass from
the list and continue.

The application will boot up on your Google Glass shortly after.

[Back to Top][BackToTop]
<br>

6 RUNNING
---------

### 6.1 Main Menu ###

Once the MA for Glass program is running, a main menu screen will show. From this
menu, the user can either open the menu by tapping the Google Glass touchpad
once, or by saying "ok glass" loud enough for the microphone to hear.

From any screen, the user can slide one finger from the top of the Google Glass
touchpad to the bottom. This will either cancel the current action or go back to
the previous list of cards. Sliding down enough times will return the user to
the main menu, and sliding down from the main menu will close the application.
Once closed, the application must be run again from Android Studio (or whichever
development environment is being used instead).

Whenever "ok glass" is written at the bottom of the screen, there are options
available on the card. Sliding a finger down on the Google Glass touchpad will
cancel menu selection when open, either by voice or by touch.

The main menu has four options:
* [Work Orders][head0602]
* [Scheduled Maintenance][head0603]
* [Scan Asset QR Code][head0604]
* [Generate Asset][head0605]

[Back to Top][BackToTop]

### 6.2 Work Orders ###

The Work Orders option loads in a list of all active work orders, sorted by
their priority from highest to lowest, and prioritizing overdue work orders.
Each work order is on a separate card, and the cards can be scrolled through by
sliding a finger across the Google Glass touchpad.

If coming from an asset card, only work orders related to the asset will be
shown.

Each work order has three options:
* [View More][head0606]
* [See Tasks][head0607]
* [Change Status][head0608]

[Back to QR Scan][head0604]  
[Back to Asset][head0605]  
[Back to Main Menu][head0601]

### 6.3 Scheduled Maintenance ###

The Scheduled Maintenance option loads in a list of all running scheduled
maintenance, sorted by their priority from highest to lowest. Each scheduled
maintenance is on a separate card, and the cards can be scrolled through by
sliding a finger across the Google Glass touchpad.

If coming from an asset card, only scheduled maintenance related to the asset
will be shown.

Each scheduled maintenance has two options:
* [View More][head0606]
* [See Tasks][head0607]

[Back to QR Scan][head0604]  
[Back to Asset][head0605]  
[Back to Main Menu][head0601]

### 6.4 Scan Asset QR Code ###

The Scan QR Code option opens the camera on Google Glass. By aiming the camera
at a QR code, the camera will try to scan the code. Scanning a valid QR code of
an asset (either the one generated by the CMMs, or one of nothing but the asset
ID) will bring up a card or two of information on the asset.

From these cards, there are four options:
* [View More][head0606]
* [Related Work Orders][head0602]
* [Related Scheduled Maintenance][head0603]
* [Generate Work Order][head0609]

[Back to Main Menu][head0601]

### 6.5 Generate Asset ###

The Generate Asset option loads a list of asset types, each one on a separate
card. The cards can be scrolled through by sliding a finger across the Google
Glass touchpad, and the desired card can be selected by tapping the Google Glass
touchpad once.

Next, the speech recognition software on Google Glass will open, and wait for
input. Whatever is said will be recorded and entered into the new asset's
description. The software stops recording when it believes the user has finished
speaking.

If the asset is successfully generated, one or two cards of information on the
asset will be loaded to the screen. This is the same as after
[scanning a QR code][head0604]. From these cards, there are four options:
* [View More][head0606]
* [Related Work Orders][head0602]
* [Related Scheduled Maintenance][head0603]
* [Create Work Request][head0609]

[Back to Main Menu][head0601]

### 6.6 View More ###

The View More option shows more information on the selected work order,
scheduled maintenance, or asset. The information is spread out across several
cards, and can be scrolled through by sliding a finger across the Google Glass
touchpad.

There are no further options from View More.

[Back to Work Orders][head0602]  
[Back to Scheduled Maintenance][head0603]  
[Back to QR Scan][head0604]  
[Back to Asset][head0605]  
[Back to Main Menu][head0601]

### 6.7 See Tasks ###

The See Tasks option loads a list of tasks related to the selected work order
or scheduled maintenance. Each card contains one task, and the list can be
scrolled through by sliding a finger across the Google Glass touchpad. By
tapping the Google Glass touchpad once, the task currently being viewed will be
completed.

* If the task result type is general, the task will be completed automatically.  
* If the task result type is text, the Google Glass voice recognition software
  will be opened and await a result. After receiving a result, the task will
  automatically complete itself.
* If the task result type is meter reading, the Google Glass voice recognition
  software will be opened and await a result. Please only speak the number,
  nothing else. After receiving a result, the task will automatically complete
  itself.

There are no further options from See Tasks.

[Back to Work Orders][head0602]  
[Back to Scheduled Maintenance][head0603]  
[Back to Main Menu][head0601]

### 6.8 Change Status ###

The Change Status option loads a list of cards, each one containing one work
order status. The cards can be scrolled through by sliding a finger across the
Google Glass touchpad.

By tapping the Google Glass touchpad once, the status currently on the screen
will be selected, and the work order which was viewed at the time of selecting
the Change Status option will have its status switched to the chosen option.

There are no further options from Change Status.

[Back to Work Order][head0602]  
[Back to Main Menu][head0601]

### 6.9 Create Work Request ###

The Create Work Request option will load a list of cards, each one containing
one pending status. This list works the same way as the Change Status option.
After selecting one status, a list of maintenance types will load, and after
selecting one maintenance type, the Google Glass voice recognition software will
be opened and await a description of the work order. After recording a summary,
when the software recognizes the user has stopped speaking, the description will
be attached to the work order.

The new work order's information is then shown on the screen. Tapping the Google
Glass touchpad will validate that the information is correct and generate the
work order. sliding a finger from the top of the Google Glass touchpad to the
bottom will cancel the process and return the user to the asset cards.

There are no further options from Create Work Request.

[Back to QR Scan][head0604]  
[Back to Asset][head0605]   
[Back to Main Menu][head0601]
<br>

7 LICENSING
-----------

The Maintenance Assistant CMMSclient for Java is licensed under the Apache
License 2.0.

See [LICENSE.txt][LCN] and [NOTICE.txt][NTC] files for
more information.

Glass is a trademark of Google Inc.

[Back to Top][BackToTop]
<br>

8 CHANGE LOG
------------

### v1.0.3 ###
* Changed "Generate Work Order" to "Create Work Request"
* Added am extra cpmfor,atopm screem tp work request generation
  * Before speech recognition
* Corrected v1.0.2 README mistakes
  * Updated the README to reflect v1.0.3 changes
* Improved consistency among status/category/maintenance type lists

### v1.0.2 ###
* Fixed multiasset work orders and scheduled maintenance
* Fixed type reading for tasks
* Changed "Scan QR Code" to "Scan Asset QR Code"
* Added status selection for generated work orders
  * Only pending statuses are shown
* Added an extra confirmation screen to asset generation
  * Before speech recognition
* Added descriptions to the asset cards
* Bug Fixes
* Updated the README to reflect the changes

### v1.0.1 ###
* Code cleanup
* README corrections
* Bug fixes
* Removed web-based image loading
* Name change due to branding

### v1.0.0 ###

* Initial upload to GitHub
* Initial project upload
* Creation and addition of a README.md

[Back to Top][BackToTop]



[head0100]: #1-authors--contributors
[head0200]: #2-introduction
[head0300]: #3-requirements
[head0400]: #4-setup
[head0500]: #5-installation
[head0600]: #6-running
[head0601]: #61-main-menu
[head0602]: #62-work-orders
[head0603]: #63-scheduled-maintenance
[head0604]: #64-scan-asset-qr-code
[head0605]: #65-generate-asset
[head0606]: #66-view-more
[head0607]: #67-see-tasks
[head0608]: #68-change-status
[head0609]: #69-create-work-request
[head0700]: #7-licensing
[head0800]: #8-change-log

[v100]: #v100
[v101]: #v101
[v102]: #v102
[v103]: #v103

[AndStd]: https://developer.android.com/sdk/index.html
[AndStp]: https://developers.google.com/glass/develop/gdk/quick-start?hl=en
[ZXing]: https://github.com/zxing/zxing
[ZXstp]: https://github.com/zxing/zxing/wiki/Getting-Started-Developing
[GitRep]: https://github.com/macmms/malabs-ma_glass
[GitZip]: https://github.com/macmms/malabs-ma_glass/archive/master.zip
[APIdoc]: https://www.maintenanceassistant.com/api/docs/guide.html

[LCN]: LICENSE.txt
[NTC]: NOTICE.txt
[STR]: app/src/main/res/values/strings.xml

[BackToTop]: #ma-for-glass
