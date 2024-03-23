# Suspicious Activity Reporting Intelligent User Interface

Suspicious Activity Reporting refers to the process by which members of the law enforcement and public safety communities as well as members of the general population communicate potentially suspicious or unlawful incidents to the appropriate authorities. SAR has been identified as one part of a broader Information Sharing Environment (ISE). The ISE initiative builds upon the foundational work by the US Departments of Justice and Homeland Security that have collaborated to create the National Information Exchange Model (NIEM).

This prototype was designed to facilitate the entry of information in NIEM-based forms dynamically generated from the corresponding schemata. A recorded can be viewed [here](http://rali.iro.umontreal.ca/gottif/sar01/demo.mp4).

It was part of larger project meant to introduce artificial Intelligence technologies: 1) to enhance human-machine interactions, to get information into the system more rapidly and also to make it more readily available to the users; and 2) for advanced machine processing to data validation, fusion and inference to be performed on data collected from multiple sources.

Implementation: Custom-made Java library using software from Apache Xerces™ Project to parse IEPD specifications (XML Schema) in order to guide the creation of an intelligent user interface. The IEPDs used are based on NIEM.

# Binary and technical documentation

You can build the project from scratch using Maven in Eclipse, or use the release directory, which also contains some documentation and essential files.

# Important

This prototype is neither complete nor is it maintained. It is provided "as-is" and no guarantee whatsoever is provided along with this code.

If the prototype is useful to you, the authors would appreciate your citing their work, [available here](http://rali.iro.umontreal.ca/rali/?q=fr/node/1470), and described below:

Gotti, Fabrizio, Heffner, Kevin, & Lapalme, Guy. (2015). XSDGuide—Automated Generation of Web Interfaces from XML Schemas: A Case Study for Suspicious Activity Reporting. Balisage 2015. [https://doi.org/10.4242/BalisageVol15.Gotti01](https://doi.org/10.4242/BalisageVol15.Gotti01)

