package aku.play.handlebars.scala

import aku.play.handlebars.HandlebarsApi
import play.api.i18n.Lang
import play.twirl.api.Content
import play.api.http.Writeable
import play.api.mvc.Codec
import play.api.http.ContentTypes

trait HandlebarsSupport {
  def handlebarsApi: HandlebarsApi
  
  /**
   * Render handlebars template with the current language
   */
  def render(templateId: String, jsonData:AnyRef)(implicit lang: Lang): Content = {
    return handlebarsApi.html(templateId, jsonData, lang.code)
  }
  
  /**
   * Write content to the result
   */
  implicit def writableHttp(implicit codec: Codec): Writeable[Content] =
    Writeable[Content]((result:Content) => codec.encode(result.body), Some(ContentTypes.HTML))
}