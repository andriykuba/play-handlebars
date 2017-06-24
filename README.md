# Handlebars template engine for Play Framework 

[![Build Status](https://travis-ci.org/andriykuba/play-handlebars.svg?branch=master)](https://travis-ci.org/andriykuba/play-handlebars) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.andriykuba/play-handlebars/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.andriykuba/play-handlebars)

This module is created for using [Handlebars](http://handlebarsjs.com/) templates with Play Framework. It uses [Handlebars.java](https://github.com/jknack/handlebars.java) under the hood with an additional resolver for the play JSON objects. Both Scala and Java are supported. 

#### Table of Contents
- [Install](#install)
- [Usage](#usage)
  - [Java](#java)
  - [Scala](#scala)
- [Play Helpers](#play-helpers)
  - [Assets](#assets)
  - [Reverse routing](#reverse-routing)
  - [Message](#message)
  - [i18n](#i18n)
- [String Helpers](#string-helpers)  
  - [Encode url parameter](#encode-url-parameter)
  - [If equals](#if-equals)
  - [Concat](#concat)
- [Scala Json Value Resolver](#scala-json-value-resolver) 

## Install

1. Add the library in `built.sbt`

    ### Play 2.6, Scala 2.12
    
    ```scala
    libraryDependencies += "com.github.andriykuba" % "play-handlebars" % "2.6.3" 
    ```

    ### Play 2.5, Scala 2.11

    ```scala
    libraryDependencies += "com.github.andriykuba" % "play-handlebars" % "2.5.11" 
    ```

        
2. Enable the module in `conf\application.conf`
    ```scala
    play.modules.enabled += "com.github.andriykuba.play.handlebars.HandlebarsModule"
    ```
    
3. Configure the templates folder and cache (optional)
    ```
    handlebars{
      directory = "/templates"   #"/templates" by default
      extension = ".hbs"         #".hbs" by default
      isCacheEnabled = true      #true by default 
    }
    ```
    
6. Configure `build.sbt` to take the templates folder in to the distribution package
    ```scala
    // Copy handlebars templates to the production
    import Path.relativeTo
    mappings in Universal ++= {
      val basePath = (baseDirectory.value).get.head
      ((baseDirectory.value / "templates" ** "*" get) pair relativeTo(basePath)) 
    }
    ```

## Usage 
### Java
Inject `HandlebarsApi` into controller and call `handlebarsApi.html(templateName, data)` method. 
 
```java
public class HomeController extends Controller { 
 
    @Inject
    private HandlebarsApi handlebarsApi;
 
    public Result index() {
        // Data. 
        final Map<String, Object> data = new HashMap<>();
        data.put("title", "Page Title");
        data.put("header", "Header");
        data.put("main", ImmutableMap.of("article", "Main Article"));
        data.put("footer", "Footer");

        // Fill it with the data.
        final Content page = handlebarsApi.html("page", data, Context.current().lang().code());

        // Return the page to the client. 
        return ok(page);
    }
}
```

### Scala
Inject `HandlebarsApi` into controller with trait `HandlebarsSupport` and call `render(templateName, data)` method.
    
```scala
class HomeController @Inject() (val handlebarsApi: HandlebarsApi)extends Controller with HandlebarsSupport{
  def index = Action { implicit request =>{
    val jsonData = 
      Json.obj("users" -> Json.arr(
        Json.obj(
          "name" -> "Jhon",
          "age" -> 4,
          "role" -> "Worker"
        ),
        Json.obj(
          "name" -> "Duck",
          "age" -> 6,
          "role" -> "Administrator"
        )))
    val page = render("page", jsonData)
    Ok(page)
  }}
}
```

In Scala version with Json data flash variables automatically added to the jsonData object.

```scala
Redirect(controllers.routes.HomeController.myForm()).flashing("success" -> "The document has been created") 
```
Previous flash variable could be extracted in a template as 

```html
<div>{{flash.success}}</div>
```
 
## Play helpers
### Assets
`assets` helper is the replacement for the twirl `@routes.Assets.versioned` method.

```html
<link rel="stylesheet" media="screen" href="{{asset "stylesheets/main.css"}}">
```

Resulting HTML:
```html
<link rel="stylesheet" media="screen" href="/assets/stylesheets/d41d8cd98f00b204e9800998ecf8427e-main.css">
```

Do not forget to configure versioning in the build file
`pipelineStages := Seq(digest)` for the production and `pipelineStages in Assets := Seq(digest)` for the development.


### Reverse routing
`route` helper is the replacement for the twirl reverse routing "&lt;full-package-name&gt;.routes.&lt;controller&gt;.&lt;action&gt;" 

```html
<form action="{{route "controllers.SecureController.loginSubmit"}}" method="POST">
```

Resulting HTML:
```html
<form action="\login" method="POST">
```

`route` helper also works with the String, Integer or variable:
 ```html
{{route "controllers.UserController.user(\"admin\")"}}

{{route "controllers.UserListController.page(42)"}}

{{route "controllers.FriendsController.friends(user.name)"}}
```

### Message
`message` helper is the replacement for the twirl `@Message(key)` method. It also could take arguments like the original method.

```html
<div>{{message "page.header.sub" "name"}}</div>
```

In the `messages.en` file
 
```
page.header.sub=Page Sub Header {0}
```

Resulting HTML:
```html
<div>Page Sub Header name</div>
```
### i18n
`messages` helper use the language that was passed to the `render` or `html` method as a language code string. This code combine in to the `handlebars` context as a `language` variable, so it can be used in template.

```html
<html lang="{{language}}">
``` 

In `scala` the `HandlebarsSupport` trait takes the language code from the implicit `Lang` variable. The implicit `request2lang` method of the Play Framework `Controller` class convert `RequestHeader` to the `Lang`. So, if you need the i18n support in the applications, you need to call your code with the implicit `request`. The example:

```scala
def index = Action { implicit request =>{
   // Your code, like render("page", jsonData), or any other that use Lang object
}}
```
## String helpers
### Encode url parameter
`encodeUrlParameter` encode the string that it could be used as URL parameter. It use `java.net.URLEncoder.encode` under the hood.

```html
<a href="https://www.google.com?q={{encodeUrlParameter "blue+light blue"}}">search</a>
```

Resulting HTML:
```html
<a href="https://www.google.com?q=blue%2Blight+blue">search</a>
```

### If equals
`if_equals` compare two objects and return the value if they are equal. 
```html
<ul>
{{#each vote.options}}
  <li><input type="radio" name="vote" value="{{@key}}" {{if_equals @key user.vote "checked"}}>{{this}}</li>
{{/each}}
</ul>
```

Resulting HTML:
```html
<ul>
  <li><input type="radio" name="vote" value="one">first</li>
  <li><input type="radio" name="vote" value="second" checked>second</li>
  <li><input type="radio" name="vote" value="third">third</li>
</ul>
```

### Concat
`concat` concatenate the string representation of the parameters in one string. 
```html
<div>{{concat "static" variable}} 
```

Resulting HTML:
```html
<div>static and some dynamic</div>
```

It useful to use in subexpressions
```html
<div>{{message (concat 'category.name.' category)}}</div>
```

## Scala Json Value Resolver
It works similar to `JsonNodeValueResolver` but resolve the classes from the `play.api.libs.json` package 