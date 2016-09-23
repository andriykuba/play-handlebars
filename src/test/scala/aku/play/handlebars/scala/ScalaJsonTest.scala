package aku.play.handlebars.scala

import collection.mutable.Stack
import org.scalatest._
import play.Configuration
import play.i18n.MessagesApi
import org.mockito.Mockito._
import aku.play.handlebars.HandlebarsApi
import org.scalatest.mockito.MockitoSugar
import java.util.HashMap
import play.api.libs.json.Json
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScalaJsonTest extends FlatSpec with Matchers with BeforeAndAfter with MockitoSugar {
  var api: HandlebarsApi = _
  val languageCode = "da"
  
  before {
    val configuration = mock[Configuration]
		val messagesApi = mock[MessagesApi]
		
		api = new HandlebarsApi(null, configuration, messagesApi)
  }
    
  "HandlebarsApi" should "work with scala code" in {
    val template = "simple template"
		val result = api.renderInline(template, new HashMap(), languageCode)
 		
		result should be (template)
  }
  
  it should "work with null data code" in {
    val template = "simple template"
		val result = api.renderInline(template, null, languageCode)
 		
		result should be (template)
  }
  
  it should "work with the play json" in {
    val template = "the value is: {{key}}"
    val data = Json.obj("key" -> "string value")
    
    val result = api.renderInline(template, data, languageCode)
    
    result should be ("the value is: string value")
  }
  
  it should "access the play json array element by an index" in {
    val template = "{{array.[0]}}{{array.[1]}}{{array.[2]}}"
    val data = Json.obj("array" -> Json.arr(1,2,3))
    
    val result = api.renderInline(template, data, languageCode)
    
    result should be ("123")
  }
  
  it should "iterate by elements of the play json array" in {
    val template = "{{#array}}{{this}}{{/array}}"
    val data = Json.obj("array" -> Json.arr(1,2,3))
    
    val result = api.renderInline(template, data, languageCode)
    
    result should be ("123")
  }
  
  it should "works with the \"each\" helper for the  play json array" in {
    val template = "{{#each array}}{{this}}{{/each}}"
    val data = Json.obj("array" -> Json.arr(1,2,3))
    
    val result = api.renderInline(template, data, languageCode)
    
    result should be ("123")
  }
  
  it should "works with array as root element" in {
    val template = "{{#this}}{{this}}{{/this}}"
    val data = Json.arr(1,2,3)
    
    val result = api.renderInline(template, data, languageCode)
    
    result should be ("123")
  }
  
  
  it should "access an property of the array element" in {
    val template = "{{array.[0].name}}"
    val data = Json.obj(
        "array" -> Json.arr(
          Json.obj("name" -> "property name"), 
          Json.obj("name" -> "property name 2")))
    
    val result = api.renderInline(template, data, languageCode)
    
    result should be ("property name")
  }
  
  it should "access object property" in {
    val template = "{{object.user.name}}"
    val data  = Json.obj(
        "object" -> Json.obj(
            "user" -> Json.obj(
                "name" -> "Petro",
                "age" -> 20)))
                
   val result = api.renderInline(template, data, languageCode)
    
    result should be ("Petro")
  }
}