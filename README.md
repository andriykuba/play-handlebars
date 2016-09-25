# Handlebars template engine for Play Framework 

[![Build Status](https://travis-ci.org/andriykuba/play-handlebars.svg?branch=master)](https://travis-ci.org/andriykuba/play-handlebars)

This module is created for using [Handlebars](http://handlebarsjs.com/) templates with Play Framework. It use [Handlebars.java](https://github.com/jknack/handlebars.java) under the hood with an additional resolver for the play JSON objects. Both Scala and Java are supported. 

## Install
1. Clone the repository

2. Go into play-handlebars directory and execute `mvn install`

3. Add the library in `built.sbt`
    ```scala
    libraryDependencies += "aku.play.handlebars" % "play-handlebars" % "2.5.9" 
    ```
    
4. Enable the module in `conf\application.conf`
    ```scala
    play.modules.enabled += "aku.play.handlebars.HandlebarsModule"
    ```
    
5. Configure the templates folder and cache (optional)
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
        final Content page = handlebarsApi.html("page", data);

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
 - @routes.Assets.versioned
 - reverse routing, i.e. "&lt;full-package-name&gt;.routes.&lt;controller&gt;.&lt;action&gt;"
 - @Message(key)
 - i18n
 
## Scala Json Value Resolver
 - JsString
 - JsNumber
 - JsBoolean
 - JsObject
 - JsArray
 - JsNull