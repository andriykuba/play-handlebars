package com.github.andriykuba.play.handlebars.scala

import com.github.andriykuba.play.handlebars.HandlebarsApi
import play.api.i18n.Lang
import play.twirl.api.Content
import play.api.http.Writeable
import play.api.mvc.Codec
import play.api.http.ContentTypes
import play.api.mvc.Flash
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json._

trait HandlebarsSupport {
  def handlebarsApi: HandlebarsApi
  
  /**
   * Render handlebars template with the current language
   * 
   * Add flash messages to the root "flash" property:
   * 
   * "{"flash":{"success":"The user has been created"}}
   */
  def render(templateId: String, jsonData:JsObject)(implicit lang: Lang, flash: Flash): Content = {
    if(flash.isEmpty){
      handlebarsApi.html(templateId, jsonData, lang.code)
    }else{
      val flashJson = Json.obj(
        "flash" -> (Json.obj() /: flash.data){(j, e) =>{
          j + (e._1 -> Json.toJson(e._2))
        }})
      val data = jsonData.deepMerge(flashJson)  
      
      handlebarsApi.html(templateId, data, lang.code)  
    }
  }
  
  /**
   * Render handlebars template with the current language
   * 
   */
  def render(templateId: String, jsonData:AnyRef)(implicit lang: Lang, flash: Flash): Content = {
    handlebarsApi.html(templateId, jsonData, lang.code)
  }
  
  /**
   * Write content to the result
   */
  implicit def writableHttp(implicit codec: Codec): Writeable[Content] =
    Writeable[Content]((result:Content) => codec.encode(result.body), Some(ContentTypes.HTML))
}