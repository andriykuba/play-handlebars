# Handlebars template engine for Play Framework 

[![Build Status](https://travis-ci.org/andriykuba/play-handlebars.svg?branch=master)](https://travis-ci.org/andriykuba/play-handlebars)

This module is created for using [Handlebars](http://handlebarsjs.com/) templates with Play Framework. It use [Handlebars.java](https://github.com/jknack/handlebars.java) under the hood with an additional resolver for the play JSON objects. Both Scala and Java are supported. 

#### Table of Contents
- [Install](#install)
- [Usage](#usage)
  - [Java](#java)
  - [Scala](#scala)
- [Helpers](#helpers)
  - [Assets](#assets)
  - [Reverse routing](#reverse-routing)
  - [Message](#message)
  - [i18n](#i18n)
  - [Scala Json Value Resolver](#scala-json-value-resolver) 

## Install

1. Add the library in `built.sbt`
    ```scala
    libraryDependencies += "com.github.andriykuba.play.handlebars" % "play-handlebars" % "2.5.1" 
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
    mappings in Universal ++=
      (baseDirectory.value / "templates" * "*" get) map
        (x => x -> ("templates/" + x.getName))
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

 
## Helpers
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

### Encode Url Parameter
`encodeUrlParameter` encode the string that it could be used as URL parameter. It use `java.net.URLEncoder.encode` under the hood.

```html
<a href="https://www.google.com?q={{encodeUrlParameter "blue+light blue"}}">search</a>
```

Resulting HTML:
```html
<a href="https://www.google.com?q=blue%2Blight+blue">search</a>
```

### Scala Json Value Resolver
It works similar to `JsonNodeValueResolver` but resolve the classes from the `play.api.libs.json` package 