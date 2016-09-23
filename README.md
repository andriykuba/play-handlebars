# Handlebars template engine for Play Framework 

[![Build Status](https://travis-ci.org/andriykuba/play-handlebars.svg?branch=master)](https://travis-ci.org/andriykuba/play-handlebars)

This module is created for using [Handlebars](http://handlebarsjs.com/) templates with Play Framework. It use [Handlebars.java](https://github.com/jknack/handlebars.java) under the hood with an additional resolver for the play JSON objects. Both Scala and Java are supported. 

## Usage 
 - Java
 - Scala
 
## Helpers
 - @routes.Assets.versioned
 - reverse routing, i.e. "<full-package-name>.routes.<controller>.<action>"
 - @Message(key)
 - i18n
 
## Scala Json Value Resolver
 - JsString
 - JsNumber
 - JsBoolean
 - JsObject
 - JsArray
 - JsNull